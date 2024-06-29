package InternetSearch;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang3.StringUtils;

import com.bit4woo.utilbox.utils.IPAddressUtils;

import InternetSearch.Client.FoFaClient;
import InternetSearch.Client.HunterClient;
import InternetSearch.Client.HunterIoClient;
import InternetSearch.Client.QuakeClient;
import InternetSearch.Client.ShodanClient;
import InternetSearch.Client.ZoomEyeClient;
import burp.BurpExtender;
import domain.DomainManager;
import domain.target.TargetTableModel;
import title.LineTableModel;

public class APISearchAction extends AbstractAction {

    /**
     *
     */
    private static final long serialVersionUID = 1933197856582351336L;
    private List<String> engineList = new ArrayList<>();

    AbstractTableModel lineModel;
    int[] modelRows;
    int columnIndex;

    private PrintWriter stdout;
    private PrintWriter stderr;

    APISearchAction() {
        try {
            stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
            stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
        } catch (Exception e) {
            stdout = new PrintWriter(System.out, true);
            stderr = new PrintWriter(System.out, true);
        }
    }

    public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, List<String> engineList,
                           boolean autoAddToTarget, boolean showInGUI) {
        super();

        if (!lineModel.getClass().equals(LineTableModel.class) && !lineModel.getClass().equals(SearchTableModel.class)
                && !lineModel.getClass().equals(TargetTableModel.class)) {
            stderr.println("wrong AbstractTableModel object");
        }

        this.lineModel = lineModel;
        this.modelRows = modelRows;
        this.columnIndex = columnIndex;
        this.engineList = engineList;
        if (engineList.size() == 1) {
            putValue(Action.NAME, "API Search On " + engineList.get(0).trim());
        } else {
            putValue(Action.NAME, "API Search On " + engineList.size() + " engines");
        }
    }

    public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, String engine, boolean autoAddToTarget, boolean showInGUI) {
        this(lineModel, modelRows, columnIndex, new ArrayList<>(Collections.singletonList(engine)), autoAddToTarget, showInGUI);
    }

    public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, String engine) {
        this(lineModel, modelRows, columnIndex, engine, false, true);
    }

    public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, List<String> engineList) {
        this(lineModel, modelRows, columnIndex, engineList, false, true);
    }

    @Override
    public final void actionPerformed(ActionEvent e) {
        SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
            @Override
            protected Map doInBackground() throws Exception {
            	Set<String> searchedContent = new HashSet<String>();
                if (modelRows.length >= 50) {
                	JOptionPane.showMessageDialog(null, "too many items selected!! should less than 50","Alert",JOptionPane.WARNING_MESSAGE);
                    stderr.print("too many items selected!! should less than 50");
                    return null;
                }
                for (int row : modelRows) {

                    String searchType = null;
                    String searchContent = null;

                    if (lineModel.getClass().equals(LineTableModel.class)) {
                        InfoTuple<String, String> result = ((LineTableModel) lineModel).getSearchTypeAndValue(row, columnIndex);
                        searchType = result.first;
                        searchContent = result.second;
                    }

                    if (lineModel.getClass().equals(SearchTableModel.class)) {
                        InfoTuple<String, String> result = ((SearchTableModel) lineModel).getSearchTypeAndValue(row, columnIndex);
                        searchType = result.first;
                        searchContent = result.second;
                    }

                    if (lineModel.getClass().equals(TargetTableModel.class)) {
                        InfoTuple<String, String> result = ((TargetTableModel) lineModel).getSearchTypeAndValue(row, columnIndex);
                        searchType = result.first;
                        searchContent = result.second;
                    }

                    String tabname = String.format("%s(%s)", searchType, searchContent);
                    
                    if (searchedContent.add(tabname)) {
                    	//保证单次操作，不对相同项进行重复搜索
                    	Set<String> already = BurpExtender.getGui().getSearchPanel().getAlreadySearchContent();
                    	if (!already.contains(tabname)) {
                    		//保证已经存在的搜索内容不再重复
                    		DoSearchAllInOn(searchType, searchContent, APISearchAction.this.engineList);
                    		System.out.println("begin search "+tabname);
                    		BurpExtender.getStdout().println("begin search "+tabname);
                    	}else {
                    		System.out.println("skip search "+tabname);
                    		BurpExtender.getStdout().println("skip search "+tabname);
                    	}
                    }else {
                    	System.out.println("skip search "+tabname);
                    	BurpExtender.getStdout().println("skip search "+tabname);
                    }
                }
                return null;
            }

            @Override
            protected void done() {

            }
        };
        worker.execute();
    }

    public static List<SearchResultEntry> DoSearch(String searchType, String searchContent, String engine) {
        List<SearchResultEntry> entries = new ArrayList<>();
        if (StringUtils.isEmpty(searchContent)) {
            return entries;
        }

        if (engine.equals(SearchEngine.FOFA)) {
            entries = new FoFaClient().SearchToGetEntry(searchContent, searchType);
        } else if (engine.equals(SearchEngine.SHODAN)) {
            entries = new ShodanClient().SearchToGetEntry(searchContent, searchType);
        } else if (engine.equals(SearchEngine.ZOOMEYE)) {
            entries = new ZoomEyeClient().SearchToGetEntry(searchContent, searchType);
        } else if (engine.equals(SearchEngine.QIANXIN_HUNTER)) {
            entries = new HunterClient().SearchToGetEntry(searchContent, searchType);
        } else if (engine.equals(SearchEngine.QIANXIN_TI)) {
            //entries = new Client().SearchToGetEntry(searchContent, searchType);
            //TODO
        } else if (engine.equals(SearchEngine.QUAKE_360)) {
            entries = new QuakeClient().SearchToGetEntry(searchContent, searchType);
        } else if (engine.equals(SearchEngine.TI_360)) {
            //entries = new Client().SearchToGetEntry(searchContent, searchType);
            //TODO
        } else if (engine.equals(SearchEngine.HUNTER_IO)) {
            entries = new HunterIoClient().SearchToGetEntry(searchContent, searchType);
        }
        //https://api.hunter.io/v2/domain-search?domain=intercom.com
        return entries;
    }

    /**
     * 多个搜索引擎 进行同类型搜索时使用，比如都搜索子域名
     *
     * @param searchType
     * @param content
     * @param engineList
     * @return
     */
    public static List<SearchResultEntry> DoSearchAllInOn(String searchType, String content, List<String> engineList) {
        return DoSearchAllInOn(searchType, content, engineList, true, false);
    }

    public static List<SearchResultEntry> DoSearchAllInOn(String searchType, String content, List<String> engineList, boolean showInGUI, boolean autoAddToTarget) {
        if (StringUtils.isEmpty(content) || StringUtils.isEmpty(searchType)) {
            BurpExtender.getStderr().print("nothing to search...");
            return null;
        }
        List<SearchResultEntry> entries = new ArrayList<>();


        for (String engine : engineList) {
            if (engine.equals(SearchEngine.HUNTER_IO)) {
                //这个逻辑有点不严谨，目前看时没问题的，后续有重大变更时注意
                searchType = SearchType.Email;
            }
            entries.addAll(APISearchAction.DoSearch(searchType, content, engine));
        }

        if (showInGUI) {
            String tabname = String.format("%s(%s)", searchType, content);
            BurpExtender.getGui().getSearchPanel().addSearchTab(tabname, entries, engineList);
        }

        //暂时不启用，之所以要设计图形界面，就是为了加入人为判断。
        autoAddToTarget = false;
        if (autoAddToTarget) {
            DomainManager result = BurpExtender.getGui().getDomainPanel().getDomainResult();
            for (SearchResultEntry entry : entries) {
                String host = entry.getHost();
                String rootDomain = entry.getRootDomain();
                result.addIfValid(host);
                List<String> ips = IPAddressUtils.grepIPv4MayPort(host);
                for (String ip : ips) {
                    if (IPAddressUtils.isValidIPv4MayPort(ip)) {
                        result.getSpecialPortTargets().add(ip);
                    }
                }
                result.getSimilarDomainSet().add(rootDomain);
            }
        }

        return entries;
    }

    public static String capitalizeFirstLetter(String str) {
        if (StringUtils.isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.toLowerCase().substring(1);
    }
}
