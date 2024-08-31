package com.compdog.rover.control.rover_control.util;

public class CurveUtils {

    /**
     * Provides an inverse curve centered around a pivot<br>
     * The pivot determines a point that the curve WILL pass through<br>
     * <small>For example, if it is needed to have a y value of 100 when x is 100
     * the pivot is (100, 100) and the negative influence determines the steepness
     * of the curve.</small><br>
     * <table>
     *     <tr>
     *          <td>
     *              <img src="./docs/desmos-graph-03.png" width="200">
     *          </td>
     *          <td>
     *              <img src="./docs/desmos-graph-2.png" width="200"">
     *          </td>
     *     </tr>
     *     <tr>
     *         <td>
     *             <center>
     *                 Pivot at (100, 100)<br>
     *                 Negative influence = 0.3
     *             </center>
     *         </td>
     *         <td>
     *             <center>
     *                 Pivot at (100, 100)<br>
     *                 Negative influence = 2.0
     *             </center>
     *         </td>
     *     </tr>
     * </table>
     * @param x Input X value
     * @param pivotX X position of the pivot
     * @param pivotY Y position of the pivot
     * @param negativeInfluence How quickly the curve falls after the pivot
     *                          <ul style="list-style-type:none">
     *                              <li>0 - no influence, straight line</li>
     *                              <li>0 to 1 - curve after the pivot has less influence than before the pivot</li>
     *                              <li>1 - equal influence before and after the pivot</li>
     *                              <li>> 1 - curve after the pivot has more influence than before the pivot</li>
     *                          <ul>
     * @return The Y value of the curve
     */
    public static double InverseCurve(double x, double pivotX, double pivotY, double negativeInfluence) {
        assert negativeInfluence >= 0;
        return Math.pow(pivotX + 1, negativeInfluence) * pivotY /
                Math.pow(x + 1, negativeInfluence);
    }

}
