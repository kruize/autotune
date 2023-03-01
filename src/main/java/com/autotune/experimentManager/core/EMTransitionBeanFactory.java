package com.autotune.experimentManager.core;

import com.autotune.experimentManager.transitions.BaseTransition;
import com.autotune.experimentManager.utils.EMUtil;

import java.lang.reflect.InvocationTargetException;

public class EMTransitionBeanFactory {
    public static BaseTransition getTransitionHandler(EMUtil.EMExpStages toStage) {
        if (null != toStage) {
            try{
                return (BaseTransition) Class.forName(toStage.getClassName()).getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
