/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.inria.papart.multitouchKinect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import processing.core.PApplet;
import toxi.geom.Matrix4x4;
import toxi.geom.Vec3D;

/**
 * This class is detecting elements close to a predifined plane.
 *
 * @author jeremy
 */
public class Touch {

    public static ArrayList<Integer> findNeighbours(int currentPoint, int halfNeigh,
            ArrayList<Integer> validPoints,
            Vec3D points[], Vec3D[] projPoints,
            int[] depth, boolean[] isValidPoints,
            boolean[] readPoints, int recLevel,
            Set<Integer> toVisit, int skip) {

        // TODO: optimisations here ?

        int x = currentPoint % MyApplet.w;
        int y = currentPoint / MyApplet.w;

        if (toVisit.contains(currentPoint)) {
            toVisit.remove(currentPoint);
        }

        ArrayList<Integer> ret = new ArrayList<Integer>();

        int max = MyApplet.w * MyApplet.h;

        for (int j = -halfNeigh; j < halfNeigh + skip; j += skip) {
            for (int i = -halfNeigh; i < halfNeigh + skip; i += skip) {

                int offset = (x + i) + (y + j) * MyApplet.w;

                // Avoid getting ouside the limits
                if (!(offset >= max
                        || (x + i) != PApplet.constrain(x + i, 0, MyApplet.w) // to big or small in X
                        || (y + j) != PApplet.constrain(y + j, 0, MyApplet.h) // to big or small in Y
                        || readPoints[offset] // already parsed point
                        || !isValidPoints[offset]
                        || projPoints[offset] == null // TODO: useless ?
                        || !Util.isInside(projPoints[offset], 0.f, 1.f)
                        || points[currentPoint].distanceTo(points[offset]) > 0.02)) {

                    readPoints[offset] = true;

                    // MyApplet.we add it to the neighbour list
                    ret.add((Integer) offset);

                    // if is is on a border
                    if (PApplet.abs(i) == halfNeigh - skip
                            || PApplet.abs(j) == halfNeigh - skip) {

                        // add to the list to examine
                        toVisit.add(offset);
                        readPoints[offset] = false;

                    } // if it is a border
                } // if is ValidPoint

            } // for j
        } // for i

        return ret;
    }

    public static ArrayList<ArrayList<Integer>> allNeighbourhood(ArrayList<Integer> validPoints,
            Vec3D points[], Vec3D[] projPoints,
            int[] depth, boolean[] isValidPoints, int skip) {

        if (validPoints == null || validPoints.isEmpty()) {
            return null;
        }

        int searchDepth = 5 * skip; // on each direction
        int searchDepth2 = 5 * skip; // on each direction

        ////  Each detected Point is going to be parsed.
        boolean readPoints[] = new boolean[MyApplet.w * MyApplet.h];
        Set<Integer> toVisit = new HashSet<Integer>();

//  currentColor = 0;
        ArrayList<ArrayList<Integer>> allNeighbourhood = new ArrayList<ArrayList<Integer>>();

        // all points are "valid"  i.e. detected in the right zone

        for (Integer v : validPoints) {
            if (!readPoints[v]) {

                ArrayList<Integer> n1 = findNeighbours(v, searchDepth, validPoints,
                        points, projPoints, depth, isValidPoints, readPoints, 0, toVisit, skip);

                while (toVisit.size() > 0) {
                    int visiting = toVisit.iterator().next();
                    n1.addAll(findNeighbours(visiting, searchDepth2, validPoints,
                            points, projPoints, depth, isValidPoints, readPoints, 3, toVisit, skip));
                }

                if (n1.isEmpty()) {
                    continue;
                }

                allNeighbourhood.add(n1);
            }
        }
        return allNeighbourhood;
    }

    public static ArrayList<TouchPoint> findMultiTouch(ArrayList<Integer> validPoints,
            Vec3D points[], Vec3D[] projPoints,
            int[] depth, boolean[] isValidPoints,
            PlaneSelection planeSelection,
            Matrix4x4 transform, int skip) {

        if (validPoints == null || validPoints.isEmpty()) {
            return null;
        }

        ArrayList<ArrayList<Integer>> allNeighbourhood = allNeighbourhood(validPoints,
                points, projPoints,
                depth, isValidPoints, skip);

        ArrayList<TouchPoint> allTouchPoints = new ArrayList<TouchPoint>();

//        int minSize = 50 / (skip * skip); // in pixels

        // TODO: Magic numbers ...
        int minSize = 10;
        float closeDistance = planeSelection.planeHeight / 5f;   // valeur indiquée dans calib * 0.05


        ClosestComparatorHeight cch = new ClosestComparatorHeight(points,planeSelection);

        // remove too small elements
        for (ArrayList<Integer> vint : allNeighbourhood) {

            if (vint.size() < minSize) {
                continue;
            }

            // sort all points
            Collections.sort(vint, cch);


            TouchPoint tp = new TouchPoint();
            tp.is3D = false;
            tp.confidence = vint.size();

            Vec3D mean = new Vec3D(0, 0, 0);
            Vec3D closeMean = new Vec3D(0, 0, 0);
            int nbClose = 0;

            for(int k=0; k < minSize; k++){
                int offset = vint.get(k);
                mean.addSelf(points[offset]);

            }

            mean.scaleSelf(1.0f / minSize);
            tp.v = mean;
            tp.vKinect = tp.v.copy();

            tp.isCloseToPlane = planeSelection.distanceTo(mean) < closeDistance;

//            System.out.println("size "+ vint.size() + " distance : " + planeSelection.distanceTo(mean) + " Confidence " + tp.confidence);
            tp.vKinect = tp.v.copy();

            tp.v = transform.applyTo(planeSelection.plane.getProjectedPoint(tp.v));
            tp.v.x /= tp.v.z;
            tp.v.y /= tp.v.z;
            tp.v.z = planeSelection.distanceTo(mean);

            allTouchPoints.add(tp);
            //tp.draw();
        }

        return allTouchPoints;
    }
}
