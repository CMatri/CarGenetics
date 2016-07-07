package com.slyth.genetics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.Array;

public class GeneticsMain extends ApplicationAdapter {
	SpriteBatch batch;
	ArrayList<Body> bars = new ArrayList<Body>();
	ArrayList<CarGene> cars = new ArrayList<CarGene>();
	OrthographicCamera cam;
	World world = new World(new Vector2(0, -9.4f), true);
	Box2DDebugRenderer debugRenderer;
	static Random rand = new Random();
	Vector2 beginPos;
	Vector2 vertex = Vector2.Zero;

	public Body createCarFromGene(CarGene gene, int x, int y) {
		Vector2[] offsets = new Vector2[gene.vertRadii.size()];
		ArrayList<PolygonShape> tris = new ArrayList<PolygonShape>();

		for (int i = 0; i < gene.vertRadii.size(); i++) {
			float radiusAtPoint = gene.vertRadii.get(i);
			float vx = (float) (radiusAtPoint * Math.cos(2 * Math.PI * i / gene.vertRadii.size()));
			float vy = (float) (radiusAtPoint * Math.sin(2 * Math.PI * i / gene.vertRadii.size()));
			offsets[i] = new Vector2(vx, vy);
		}

		for (int i = 0; i < gene.vertRadii.size(); i++) {
			PolygonShape s = new PolygonShape();
			s.set(new Vector2[] { offsets[i], new Vector2(0, 0), i == gene.vertRadii.size() - 1 ? offsets[0] : offsets[i + 1] });
			tris.add(s);
		}

		Filter f = new Filter();
		f.categoryBits = 0x02;
		f.maskBits = 0x01;

		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(x, y);
		bodyDef.type = BodyType.DynamicBody;
		bodyDef.gravityScale = 1.5f;
		bodyDef.linearDamping = 0.0f;
		Body carBody = world.createBody(bodyDef);

		for (int i = 0; i < tris.size(); i++) {
			carBody.createFixture(tris.get(i), 10.0f);
			carBody.getFixtureList().get(i).setFilterData(f);
		}

		CircleShape wheelShape = new CircleShape();
		FixtureDef wheelFixtureDef = new FixtureDef();
		wheelFixtureDef.density = 100;

		for (int i = 0; i < gene.wheelVerts.size(); i++) {
			wheelShape.setRadius(gene.wheelRadii.get(i));
			wheelFixtureDef.shape = wheelShape;
			wheelFixtureDef.filter.categoryBits = f.categoryBits;
			wheelFixtureDef.filter.maskBits = f.maskBits;
			wheelFixtureDef.friction = 1f;
			Body wheelBody = world.createBody(bodyDef);
			wheelBody.createFixture(wheelFixtureDef);

			RevoluteJointDef axisDef = new RevoluteJointDef();
			axisDef.bodyA = carBody;
			axisDef.bodyB = wheelBody;
			axisDef.collideConnected = false;
			try {
				axisDef.localAnchorA.set(offsets[gene.wheelVerts.get(i)]);
			} catch (ArrayIndexOutOfBoundsException e) {
				e.printStackTrace();
				System.exit(0);
			}
			axisDef.localAnchorB.set(0, 0);
			axisDef.enableMotor = gene.wheelIsMotor.get(i);
			axisDef.maxMotorTorque = 200000;
			axisDef.motorSpeed = gene.wheelSpeed.get(i);
			RevoluteJoint axis = (RevoluteJoint) world.createJoint(axisDef);
		}

		gene.chassis = carBody;

		return carBody;
	}

	float farthestLength;

	public void triggerBreed() {
		CarGene[] c = new CarGene[cars.size()];

		c = cars.toArray(c);

		Arrays.sort(c, new Comparator<CarGene>() {
			@Override
			public int compare(CarGene x, CarGene y) {
				return (int) (x.chassis.getPosition().x - y.chassis.getPosition().x);
			}
		});

		float totalFitness = 0;

		for (int i = c.length - 1; i >= 0; i--) {
			float fitness = (float) Math.pow(beginPos.x - c[i].chassis.getWorldCenter().x, 2);

			if (fitness <= 0) {
				c[i].fitness = -1;
				continue;
			}

			totalFitness += fitness;
			c[i].fitness = fitness;
		}

		ArrayList<CarGene> choices = new ArrayList<CarGene>();

		for (int i = 0; i < c.length; i++) {
			if (c[i].fitness == -1)
				continue;

			int numEntries = Math.round((c[i].fitness / totalFitness) * 1000);

			for (int j = 0; j < numEntries && choices.size() < 1000; j++) {
				choices.add(c[i]);
			}
		}

		CarGene[] production = new CarGene[cars.size()];

		CarGene one, two;

		for (int i = 0; i < production.length; i++) {
			one = choices.get(randInt(0, choices.size() - 1));
			do {
				two = choices.get(randInt(0, choices.size() - 1));
			} while (one == two);

			production[i] = one.breed(two);
		}

		Array<Body> bodies = new Array<Body>();
		world.getBodies(bodies);
		for (int i = 0; i < bodies.size; i++) {
			if (!world.isLocked() && bodies.get(i).getType() == BodyType.DynamicBody)
				world.destroyBody(bodies.get(i));
		}

		int pastSize = cars.size();
		cars.clear();

		for (int i = 0; i < pastSize; i++) {
			if (i > 0/* pastSize / 4 */) {
				createCarFromGene(production[i], (int) vertex.x - 50, (int) vertex.y + 50);
				cars.add(production[i]);
			} else {
				CarGene gene = CarGene.genRandomCar();
				createCarFromGene(gene, (int) vertex.x - 50, (int) vertex.y + 50);
				cars.add(gene);
			}
		}
	}

	@Override
	public void create() {
		Box2D.init();
		batch = new SpriteBatch();
		debugRenderer = new Box2DDebugRenderer();

		cam = new OrthographicCamera(30, 30 * (Gdx.graphics.getHeight() / Gdx.graphics.getWidth()));
		cam.position.set(cam.viewportWidth / 4f, cam.viewportHeight / 4f, 0);
		cam.update();
		cam.zoom = 20;

		BodyDef groundBodyDef = new BodyDef();
		EdgeShape edgeShape = new EdgeShape();
		Body groundBody = null;
		int lastX = 0;

		int numBars = 100;

		for (int i = 0; i < numBars; i++) {
			int width = randInt(0, 60);

			Vector2 pos;

			if (i == 0) {
				pos = new Vector2(0, 0);
			} else {
				EdgeShape pShape = (EdgeShape) bars.get(i - 1).getFixtureList().get(0).getShape();
				pShape.getVertex2(vertex);
				bars.get(i - 1).getTransform().mul(vertex);
				bars.get(i - 1).getWorldPoint(vertex);
				pos = new Vector2(vertex);
			}

			int factor = (int) (0.6f * (numBars - i));

			groundBodyDef.position.set(pos);
			groundBodyDef.angle = randInt(-30 - factor, 30 + factor) * MathUtils.degreesToRadians;
			groundBody = world.createBody(groundBodyDef);
			edgeShape.set(0, 0, width, 0);
			groundBody.createFixture(edgeShape, 0.0f);
			groundBody.getFixtureList().get(0).setFriction(1f);

			edgeShape.getVertex2(vertex);
			groundBody.getTransform().mul(vertex);
			groundBody.getWorldPoint(vertex);

			cam.position.set(new Vector3(pos.x, pos.y, 1));
			beginPos = pos;

			bars.add(groundBody);
		}

		for (int i = 0; i < 30; i++) {
			CarGene gene = CarGene.genRandomCar();
			cars.add(gene);
			createCarFromGene(gene, (int) vertex.x - 50, (int) vertex.y + 50);
		}

		edgeShape.dispose();
	}

	@Override
	public void resize(int width, int height) {
		cam.viewportWidth = 30f;
		cam.viewportHeight = 30f * height / width;
		cam.update();
	}

	@Override
	public void render() {
		// bars.get(0).setTransform(0, 0, bars.get(0).getAngle() + 0.01f);

		handleInput();
		cam.update();

		Gdx.gl.glClearColor(0.25f, 0.25f, 0.25f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.setProjectionMatrix(cam.combined);
		batch.begin();
		batch.end();

		debugRenderer.render(world, cam.combined);
		world.step(1 / 60f, 6, 2);
	}

	@Override
	public void dispose() {
		batch.dispose();
	}

	public static int randInt(int min, int max) {
		int randomNum = rand.nextInt((max - min) + 1) + min;
		return randomNum;
	}

	private void handleInput() {
		if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
			cam.zoom += 0.2;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.E)) {
			cam.zoom -= 0.2;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.A)) {
			cam.translate(-9, 0, 0);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.D)) {
			cam.translate(9, 0, 0);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.S)) {
			cam.translate(0, -9, 0);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.W)) {
			cam.translate(0, 9, 0);
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
			triggerBreed();
		}
	}

	static class Car {
		CarGene gene;
	}

	static class CarGene {
		private static final int vertNumMin = 4;
		private static final int vertNumMax = 8;
		private static final int vertRadiiMin = 1;
		private static final int vertRadiiMax = 20;
		private static final int wheelNumMin = 1;
		private static final int wheelRadiiMin = 1;
		private static final int wheelRadiiMax = 5;
		private static final int wheelSpeedMin = 0;
		private static final int wheelSpeedMax = 500;

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

			ArrayList<Integer> newWheelVerts = rand.nextBoolean() ? (ArrayList<Integer>) mate.wheelVerts.clone() : (ArrayList<Integer>) wheelVerts.clone();

			if (wheelVerts.size() != wheelVerts.size() || wheelRadii.size() != wheelVerts.size() || wheelSpeed.size() != wheelVerts.size() || wheelIsMotor.size() != wheelVerts.size()) {
				System.out.println("Invalid host, crash imminent.");
				System.exit(-1);
			} else if (mate.wheelVerts.size() != mate.wheelVerts.size() || mate.wheelRadii.size() != mate.wheelVerts.size() || mate.wheelSpeed.size() != mate.wheelVerts.size() || mate.wheelIsMotor.size() != mate.wheelVerts.size()) {
				System.out.println("Invalid mate, crash imminent.");
				System.exit(-1);
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
			int numVerts = randInt(vertNumMin, vertNumMax);
			ArrayList<Integer> vertRadii = new ArrayList<Integer>();

			for (int i = 0; i < numVerts; i++) {
				vertRadii.add(randInt(vertRadiiMin, vertRadiiMax));
			}

			int numWheels = randInt(wheelNumMin, vertRadii.size());

			ArrayList<Integer> wheelVertices = new ArrayList<Integer>(numWheels);
			int vert;

			for (int i = 0; i < numWheels; i++) {
				do {
					vert = randInt(0, vertRadii.size() - 1);
				} while (wheelVertices.contains(vert));

				wheelVertices.add(vert);
			}

			ArrayList<Integer> wheelRadii = new ArrayList<Integer>(numWheels);
			ArrayList<Integer> wheelSpeed = new ArrayList<Integer>(numWheels);
			ArrayList<Boolean> wheelIsMotor = new ArrayList<Boolean>(numWheels);

			for (int i = 0; i < numWheels; i++) {
				wheelRadii.add(randInt(wheelRadiiMin, wheelRadiiMax));
				wheelSpeed.add(randInt(wheelSpeedMin, wheelSpeedMax));
				wheelIsMotor.add(rand.nextBoolean());
			}

			return new CarGene(vertRadii, wheelVertices, wheelRadii, wheelSpeed, wheelIsMotor);
		}
	}
}
