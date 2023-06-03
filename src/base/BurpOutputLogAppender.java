package base;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

//https://stackoverflow.com/questions/24205093/how-to-create-a-custom-appender-in-log4j2
//https://cwiki.apache.org/confluence/display/GEODE/Using+Custom+Log4J2+Appender

//TODO
@Plugin(name = "Basic", category = "Core", elementType = "appender", printObject = true)
public class BurpOutputLogAppender extends AbstractAppender {

	private static volatile BurpOutputLogAppender instance;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    
	public BurpOutputLogAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout) {
		super(name, filter, layout);
	}

	@PluginFactory
	public static BurpOutputLogAppender createAppender(@PluginAttribute("name") String name,
			@PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
			@PluginElement("Layout") Layout layout,
			@PluginElement("Filters") Filter filter) {
		if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}

		instance = new BurpOutputLogAppender(name, filter, layout);
		return instance;
	}

	public static BurpOutputLogAppender getInstance() {
		return instance;
	}

	@Override
	public void append(final LogEvent event) {
		readLock.lock();
        try {
            final byte[] bytes = getLayout().toByteArray(event);
            System.out.write(bytes);
        } catch (Exception ex) {
            if (!ignoreExceptions()) {
                throw new AppenderLoggingException(ex);
            }
        } finally {
            readLock.unlock();
        }
	}
}