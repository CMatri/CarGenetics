package com.slyth.genetics;

import com.badlogic.gdx.physics.box2d.Body;

import java.util.ArrayList;

class CarGene {
    private static final int vertNumMin = 3;
    private static final int vertNumMax = 20;
    private static final int vertRadiiMin = 1;
    private static final int vertRadiiMax = 20;
    private static final int wheelNumMin = 1;
    private static final int wheelRadiiMin = 1;
    private static final int wheelRadiiMax = 10;
    private static final int wheelSpeedMin = 0;
    private static final int wheelSpeedMax = 100;

    ArrayList<Integer> vertRadii;
    ArrayList<Integer> wheelVerts;
    ArrayList<Integer> wheelRadii;
    ArrayList<Integer> wheelSpeed;
    ArrayList<Boolean> wheelIsMotor;
    float fitness;
    Body chassis;

    public CarGene(ArrayList<Integer> vertRadii, ArrayList<Integer> wheelVerts, ArrayList<Integer> wheelRadii, ArrayList<Integer> wheelSpeed, ArrayList<Boolean> wheelIsMotor) {
        this.vertRadii = vertRadii;
        this.wheelVerts = wheelVerts;
        this.wheelRadii = wheelRadii;
        this.wheelSpeed = wheelSpeed;
        this.wheelIsMotor = wheelIsMotor;
    }

    public boolean testValidity() {
        return (wheelRadii.size() == wheelVerts.size() &&
                wheelSpeed.size() == wheelVerts.size() &&
                wheelIsMotor.size() == wheelVerts.size() &&
                wheelVerts.size() <= vertRadii.size() &&
                wheelSpeed.size() == wheelIsMotor.size() &&
                wheelSpeed.size() == wheelRadii.size() &&
                wheelRadii.size() == wheelIsMotor.size());
    }

    public CarGene breed(CarGene mate) {
        int newNumVerts = (mate.vertRadii.size() + vertRadii.size()) / 2;
        ArrayList<Integer> newVertRadii = new ArrayList<Integer>(newNumVerts);

        for (int i = 0; i < newNumVerts; i++) {
            if (i >= mate.vertRadii.size())
                newVertRadii.add(vertRadii.get(i));
            else if (i >= vertRadii.size())
                newVertRadii.add(mate.vertRadii.get(i));
            else if (i < vertRadii.size() && i < mate.vertRadii.size())
                newVertRadii.add((mate.vertRadii.get(i) + vertRadii.get(i)) / 2);
        }

        int newNumWheels = (mate.wheelVerts.size() + wheelVerts.size()) / 2;

        if (newNumWheels > newNumVerts)
            newNumWheels = newNumVerts;

        ArrayList<Integer> newWheelRadii = new ArrayList<Integer>(newNumWheels);
        ArrayList<Integer> newWheelSpeed = new ArrayList<Integer>(newNumWheels);
        ArrayList<Boolean> newWheelIsMotor = new ArrayList<Boolean>(newNumWheels);

        for (int i = 0; i <= newNumWheels; i++) {
            if (i >= mate.wheelVerts.size() && i < wheelVerts.size()) {
                newWheelRadii.add(wheelRadii.get(i));
                newWheelSpeed.add(wheelSpeed.get(i));
                newWheelIsMotor.add(wheelIsMotor.get(i));
            } else if (i >= wheelVerts.size() && i < mate.wheelVerts.size()) {
                newWheelRadii.add(mate.wheelRadii.get(i));
                newWheelSpeed.add(mate.wheelSpeed.get(i));
                newWheelIsMotor.add(mate.wheelIsMotor.get(i));
            } else if (i < mate.wheelVerts.size() && i < wheelVerts.size()) {
                newWheelRadii.add((mate.wheelRadii.get(i) + wheelRadii.get(i)) / 2);
                newWheelSpeed.add((mate.wheelSpeed.get(i) + wheelSpeed.get(i)) / 2);
                newWheelIsMotor.add(mate.wheelIsMotor.get(i) || wheelIsMotor.get(i));
            }
        }

        ArrayList<Integer> newWheelVerts = Util.rand.nextBoolean() ? (ArrayList<Integer>) mate.wheelVerts.clone() : (ArrayList<Integer>) wheelVerts.clone();
        if (newWheelVerts.size() > newNumWheels) {
            newWheelVerts.subList(newNumWheels, newWheelVerts.size()).clear();
        }

        while (newWheelVerts.size() < newNumWheels) {
            int vert = 0;

            do {
                vert = Util.randInt(0, newVertRadii.size() - 1);
            } while (newWheelVerts.contains(vert));

            newWheelVerts.add(vert);
        }

        if (!testValidity()) {
            System.out.println("Invalid host, crash imminent.");
            //System.exit(-1);
        } else if (!mate.testValidity()) {
            System.out.println("Invalid mate, crash imminent.");
            //System.exit(-1);
        }

        for (int i = 0; i < newWheelVerts.size(); i++) {
            if (newWheelVerts.get(i) >= newVertRadii.size()) {
                int vert = 0;

                do {
                    vert = Util.randInt(0, newVertRadii.size() - 1);
                } while (newWheelVerts.contains(vert));

                newWheelVerts.set(i, vert);
            }
        }

        return new CarGene(newVertRadii, newWheelVerts, newWheelRadii, newWheelSpeed, newWheelIsMotor);
    }

		/*
         * public String getGeneString() { String gene = numVerts + ":";
		 *
		 * for(int i = 0; i < numVerts; i++) gene += vertRadii[i] + ","; gene +=
		 * ":" + numWheels; for(int i = 0; i < numWheels; i++) gene +=
		 * wheelVerts[i] + ","; gene += ":"; for(int i = 0; i < numWheels; i++)
		 * gene += wheelRadii[i] + ","; gene += ":"; for(int i = 0; i <
		 * numWheels; i++) gene += wheelSpeed[i] + ","; gene += ":"; for(int i =
		 * 0; i < numWheels; i++) gene += (wheelIsMotor[i] ? "1" : "0") + ",";
		 *
		 * return gene; }
		 */

    public static CarGene genRandomCar() {
        int numVerts = Util.randInt(vertNumMin, vertNumMax);
        ArrayList<Integer> vertRadii = new ArrayList<Integer>();

        for (int i = 0; i < numVerts; i++) {
            vertRadii.add(Util.randInt(vertRadiiMin, vertRadiiMax));
        }

        int numWheels = Util.randInt(wheelNumMin, vertRadii.size());

        ArrayList<Integer> wheelVertices = new ArrayList<Integer>(numWheels);
        int vert;

        for (int i = 0; i < numWheels; i++) {
            do {
                vert = Util.randInt(0, vertRadii.size() - 1);
            } while (wheelVertices.contains(vert));

            wheelVertices.add(vert);
        }

        ArrayList<Integer> wheelRadii = new ArrayList<Integer>(numWheels);
        ArrayList<Integer> wheelSpeed = new ArrayList<Integer>(numWheels);
        ArrayList<Boolean> wheelIsMotor = new ArrayList<Boolean>(numWheels);

        for (int i = 0; i < numWheels; i++) {
            wheelRadii.add(Util.randInt(wheelRadiiMin, wheelRadiiMax));
            wheelSpeed.add(Util.randInt(wheelSpeedMin, wheelSpeedMax));
            wheelIsMotor.add(Util.rand.nextBoolean());
        }

        return new CarGene(vertRadii, wheelVertices, wheelRadii, wheelSpeed, wheelIsMotor);
    }
}
