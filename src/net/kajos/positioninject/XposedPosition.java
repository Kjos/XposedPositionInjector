package net.kajos.positioninject;

import android.opengl.Matrix;
import android.util.Log;
import de.robv.android.xposed.*;
import static de.robv.android.xposed.XposedHelpers.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

/**
 * Created by Kajos on 13-1-2016.
 */
public class XposedPosition implements IXposedHookLoadPackage {
    public static final String TAG = "TranslationInjection";

    public static final float SPEED = .005f;
    public static final float HEIGHT = 1f;

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.d(TAG, "Package: " + lpparam.packageName);

        try {
            Method m;

            Class clazz = XposedHelpers.findClass("com.google.vrtoolkit.cardboard.Eye", lpparam.classLoader);

            m = XposedHelpers.findMethodExact(clazz, "getEyeView");
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                float[] transmat = new float[16];
                float[] tmp = new float[16];

                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    float[] res = (float[]) param.getResult();

                    long time = System.currentTimeMillis();
                    float diff = (float) Math.sin((float)(time % 1000) * SPEED);
                    float offset = -HEIGHT + HEIGHT * diff;

                    Matrix.setIdentityM(transmat, 0);
                    transmat[12] = 0;
                    transmat[13] = offset;
                    transmat[14] = 0;

                    Matrix.multiplyMM(tmp, 0, res, 0, transmat, 0);
                    System.arraycopy(tmp, 0, res, 0, 16);

                    // Not necessary probably
                    param.setResult(res);
                }
            });

            clazz = XposedHelpers.findClass("com.google.vrtoolkit.cardboard.HeadTransform", lpparam.classLoader);

            m = XposedHelpers.findMethodExact(clazz, "getHeadView", float[].class, int.class);
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                float[] transmat = new float[16];
                float[] tmp = new float[16];

                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    //Log.d(TAG, "Hook 3");

                    long time = System.currentTimeMillis();
                    float diff = (float) Math.sin((float)(time % 1000) * SPEED);
                    float offset = -HEIGHT + HEIGHT * diff;

                    float[] res = (float[])param.args[0];

                    Matrix.setIdentityM(transmat, 0);
                    transmat[12] = 0;
                    transmat[13] = offset;
                    transmat[14] = 0;

                    Matrix.multiplyMM(tmp, 0, res, 0, transmat, 0);
                    System.arraycopy(tmp, 0, res, 0 , 16);
                    //customRenderer.passHeadTransform(res);
                }
            });

            Log.d(TAG, "Hooked successfully!");
        } catch (ClassNotFoundError notfound) {
            // Do nothing
        }catch (Throwable t) {
            Log.e(TAG, "Exception in hook: " + t.getMessage());
            Log.e(TAG, "Cause: " + t.getCause());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            Log.e(TAG, "Trace: " + sw.toString());
            // Do nothing
        }
    }
}