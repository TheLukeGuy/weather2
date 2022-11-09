package sh.lpx.weather2.client.render.shader;


import sh.lpx.weather2.util.WeatherUtilMath;
import org.lwjgl.util.vector.Quaternion;

import javax.vecmath.Vector3f;

public class Transformation {

    private Matrix4fe modelViewMatrix;

    public Matrix4fe modelMatrix;

    public Transformation() {
        modelViewMatrix = new Matrix4fe();
        modelMatrix = new Matrix4fe();
    }

    public Matrix4fe buildModelViewMatrix(Matrix4fe modelMatrix, Matrix4fe viewMatrix) {
        /*Matrix4fe viewCurr = new Matrix4fe(viewMatrix);
        return viewCurr.mul(modelMatrix);*/
        return viewMatrix.mulAffine(modelMatrix, modelViewMatrix);
    }

    public Matrix4fe buildModelMatrix(IShaderRenderedEntity gameItem, Vector3f posCustom, float partialTicks) {
        Quaternion q = gameItem.getQuaternion();
        if (gameItem.getQuaternionPrev() != null) {
            q = WeatherUtilMath.interpolate(gameItem.getQuaternionPrev(), gameItem.getQuaternion(), partialTicks);
            //q = gameItem.getQuaternionPrev();
        }

        float scaleAdj = gameItem.getScale();

        //???
        scaleAdj *= 0.2F;

        Vector3f vecPos = posCustom != null ? posCustom : gameItem.getPosition();

        return modelMatrix.translationRotateScale(
                vecPos.x, vecPos.y, vecPos.z,
                q.x, q.y, q.z, q.w,
                scaleAdj, scaleAdj, scaleAdj);
    }
}
