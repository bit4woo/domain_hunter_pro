package domain;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import GUI.JScrollPanelWithHeader;
import burp.BurpExtender;
import com.bit4woo.utilbox.utils.SwingUtils;
import dao.DbWriter;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * 保存TextArea的变更
 * <p>
 * 改造要点（原实现每敲一个键就 new DomainDao + 整块重写一次 JSON）：
 * </p>
 * <ol>
 *   <li><b>不再每次按键都直接写库</b>。一个 5 万条子域名的项目，敲一个键就是一次
 *       5 万字符串的序列化 + 一次几 MB 的行重写；连续输入时这个成本是按"每个字符"计的。</li>
 *   <li><b>改为交给 {@link DbWriter}</b>：入队后由写线程在 linger 窗口（300ms）里合并，
 *       用户停止输入后才真正落盘一次。这就是 debounce，不需要额外的 Timer。</li>
 *   <li><b>内存仍然是真源</b>：{@code fillContentByType} 依然同步执行，
 *       DomainManager 始终是最新状态，界面和后续逻辑读到的数据不会滞后。</li>
 * </ol>
 * <p>
 * 代价：程序在"用户刚敲完最后一个字符、还没来得及落盘"的 300ms 内崩溃，会丢这一次编辑。
 * 这是可接受的权衡——原来虽然立刻写库，但每敲一个键就卡一次 UI，且写放大严重。
 * 需要"确定落盘"的场景（保存项目、备份、导入）都会显式调用 {@code DbWriter.flush()}。
 * </p>
 */
public class TextAreaListener implements DocumentListener {
	JScrollPanelWithHeader TextAreaPanel;
	private DomainPanel domainPanel;

	public TextAreaListener(DomainPanel domainPanel, JScrollPanelWithHeader panel){
		this.domainPanel = domainPanel;
		this.TextAreaPanel = panel;
	}

	/**
	 * 当UI中的数据发生变化时,需要同步到数据模型、并写入数据库
	 */
	public void saveToDBAndSyncModel(){
		if (!domainPanel.isListenerIsOn()){
			return;
		}
		if (domainPanel.getDomainResult() == null) {// 尚未打开项目
			return;
		}
		File dbFile = BurpExtender.getDataLoadManager().getCurrentDBFile();
		if (dbFile == null) {
			return;
		}

		Set<String> content = new HashSet<>(SwingUtils.getLinesFromTextArea(TextAreaPanel.getTextArea()));
		TextAreaType type = TextAreaPanel.getTextAreaType();

		// 先更新内存模型（真源），再异步入队落库
		domainPanel.getDomainResult().fillContentByType(type,content);
		DbWriter.getInstance().saveDomainType(dbFile.getPath(), type, content);

		// 这里刻意不调用 markContentSaved()：
		// 计算内容签名是 O(元素总数)，放在按键路径上等于"每敲一个键扫一遍全部子域名"，
		// 正是这次改造要消除的成本。代价是每 2 分钟的自动保存可能把同样的内容再写一次
		// （幂等写入，且已由后续的批量/合并机制削平），相比按键卡顿完全可以接受。
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		saveToDBAndSyncModel();

	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		saveToDBAndSyncModel();
	}

	@Override
	public void changedUpdate(DocumentEvent arg0) {
		saveToDBAndSyncModel();
	}
}
