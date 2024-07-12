package domain.target;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MethodInvoker {

    /**
     * 调用对象的指定方法
     * @param obj 要调用方法的对象
     * @param methodName 要调用的方法名
     * @param params 方法所需的参数
     * @return 调用结果
     */
    public static Object invokeMethod(Object obj, String methodName, Object... params) {
        try {
            Method method = getMethod(obj.getClass(), methodName, params);
            if (method != null) {
                return method.invoke(obj, params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 调用多个对象的指定方法
     * @param objects 要调用方法的对象列表
     * @param methodName 要调用的方法名
     * @param params 方法所需的参数
     * @return 调用结果列表
     */
    public static List<Object> invokeMethod(List<Object> objects, String methodName, Object... params) {
        List<Object> results = new ArrayList<>();

        for (Object obj : objects) {
            try {
                Method method = getMethod(obj.getClass(), methodName, params);
                if (method != null) {
                    Object result = method.invoke(obj, params);
                    results.add(result);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return results;
    }

    /**
     * 获取指定方法
     * @param clazz 类对象
     * @param methodName 方法名
     * @param params 方法参数
     * @return 方法对象
     * @throws NoSuchMethodException
     */
    private static Method getMethod(Class<?> clazz, String methodName, Object... params) throws NoSuchMethodException {
        Class<?>[] paramTypes = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }
        return clazz.getMethod(methodName, paramTypes);
    }
}
