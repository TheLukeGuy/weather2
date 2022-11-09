package sh.lpx.weather2.util;

import net.minecraft.util.math.Quaternion;

/**
 * Created by corosus on 29/05/17.
 */
public class WeatherUtilMath {
    final static double EPS = 0.000001;

    public static Quaternion rotation(Quaternion q, float angleX, float angleY, float angleZ) {
        double thetaX = (double) angleX * 0.5D;
        double thetaY = (double) angleY * 0.5D;
        double thetaZ = (double) angleZ * 0.5D;
        double thetaMagSq = thetaX * thetaX + thetaY * thetaY + thetaZ * thetaZ;
        double s;
        if (thetaMagSq * thetaMagSq / 24.0D < 9.99999993922529E-9D) {
            q.w = (float) (1.0D - thetaMagSq / 2.0D);
            s = 1.0D - thetaMagSq / 6.0D;
        } else {
            double thetaMag = Math.sqrt(thetaMagSq);
            double sin = Math.sin(thetaMag);
            s = sin / thetaMag;
            q.w = (float) cosFromSin(sin, thetaMag);
        }

        q.x = (float) (thetaX * s);
        q.y = (float) (thetaY * s);
        q.z = (float) (thetaZ * s);
        return q;
    }

    public static double cosFromSin(double sin, double angle) {
        return Math.sin(angle + 1.5707963267948966D);
    }

    /**
     * Performs a great circle interpolation between quaternion q1
     * and quaternion q2 and places the result into this quaternion.
     *
     * @param q1    the first quaternion
     * @param q2    the second quaternion
     * @param alpha the alpha interpolation parameter
     */
    public static Quaternion interpolate(Quaternion q1, Quaternion q2, float alpha) {
        // From "Advanced Animation and Rendering Techniques"
        // by Watt and Watt pg. 364, function as implemented appeared to be
        // incorrect.  Fails to choose the same quaternion for the double
        // covering. Resulting in change of direction for rotations.
        // Fixed function to negate the first quaternion in the case that the
        // dot product of q1 and this is negative. Second case was not needed.

        double dot, s1, s2, om, sinom;

        dot = q2.getX() * q1.getX() + q2.getY() * q1.getY() + q2.getZ() * q1.getZ() + q2.getW() * q1.getW();

        if (dot < 0) {
            // negate quaternion
            q1.x = -q1.x;
            q1.y = -q1.y;
            q1.z = -q1.z;
            q1.w = -q1.w;
            dot = -dot;
        }

        if ((1.0 - dot) > EPS) {
            om = Math.acos(dot);
            sinom = Math.sin(om);
            s1 = Math.sin((1.0 - alpha) * om) / sinom;
            s2 = Math.sin(alpha * om) / sinom;
        } else {
            s1 = 1.0 - alpha;
            s2 = alpha;
        }

        return new Quaternion((float) (s1 * q1.getX() + s2 * q2.getX()), (float) (s1 * q1.getY() + s2 * q2.getY()), (float) (s1 * q1.getZ() + s2 * q2.getZ()), (float) (s1 * q1.getW() + s2 * q2.getW()));
    }
}
