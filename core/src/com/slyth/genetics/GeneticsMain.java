package com.slyth.genetics;

import java.util.ArrayList;
import java.util.Random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Intersector;
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
import com.badlogic.gdx.physics.box2d.joints.WheelJoint;
import com.badlogic.gdx.physics.box2d.joints.WheelJointDef;
import com.badlogic.gdx.utils.Array;

public class GeneticsMain extends ApplicationAdapter {
	SpriteBatch batch;
	ArrayList<Body> bars = new ArrayList<Body>();
	ArrayList<CarGene> cars;
	OrthographicCamera cam;
	World world = new World(new Vector2(0, -9.4f), true);
	Box2DDebugRenderer debugRenderer;
	static Random rand = new Random();

	public Body createCarFromGene(CarGene gene, int x, int y) {
		Vector2[] offsets = new Vector2[gene.numVerts];

		for (int i = 0; i < gene.numVerts; i++) {
			float radiusAtPoint = gene.vertRadii[i];
			float vx = (float) (radiusAtPoint * Math.cos(2 * Math.PI * i / gene.numVerts));
			float vy = (float) (radiusAtPoint * Math.sin(2 * Math.PI * i / gene.numVerts));
			offsets[i] = new Vector2(vx, vy);
		}

		PolygonShape shape = new PolygonShape();
		shape.set(offsets);

		Filter f = new Filter();
		f.categoryBits = 0x02;
		f.maskBits = 0x01;

		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(x, y);
		bodyDef.type = BodyType.DynamicBody;
		bodyDef.gravityScale = 2.5f;
		bodyDef.linearDamping = 0.0f;
		Body carBody = world.createBody(bodyDef);
		carBody.createFixture(shape, 10.0f);
		carBody.getFixtureList().get(0).setFilterData(f);

		CircleShape wheelShape = new CircleShape();
		FixtureDef wheelFixtureDef = new FixtureDef();
		wheelFixtureDef.density = 100;

		for (int i = 0; i < gene.numWheels; i++) {
			wheelShape.setRadius(gene.wheelRadii[i]);
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
			axisDef.localAnchorA.set(offsets[gene.wheelVerts[i]]);
			axisDef.localAnchorB.set(0, 0);
			axisDef.enableMotor = gene.wheelIsMotor[i];
			axisDef.maxMotorTorque = 200000;
			axisDef.motorSpeed = gene.wheelSpeed[i];
			RevoluteJoint axis = (RevoluteJoint) world.createJoint(axisDef);
		}

		gene.chassis = carBody;
		return carBody;
	}

	public void triggerBreed() {
		
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
		Vector2 vertex = Vector2.Zero;
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
			System.out.println(i + " : " + factor);
			groundBodyDef.angle = randInt(-30 - factor, 30 + factor) * MathUtils.degreesToRadians;
			groundBody = world.createBody(groundBodyDef);
			edgeShape.set(0, 0, width, 0);
			groundBody.createFixture(edgeShape, 0.0f);
			groundBody.getFixtureList().get(0).setFriction(1f);

			edgeShape.getVertex2(vertex);
			groundBody.getTransform().mul(vertex);
			groundBody.getWorldPoint(vertex);

			cam.position.set(new Vector3(pos.x, pos.y, 1));

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
		if (Gdx.input.isKeyPressed(Input.Keys.B)) {
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

		int numVerts;
		int[] vertRadii;
		int numWheels;
		int[] wheelVerts;
		int[] wheelRadii;
		int[] wheelSpeed;
		boolean[] wheelIsMotor;
		Body chassis;

		public CarGene(int numVerts, int[] vertRadii, int numWheels, int[] wheelVerts, int[] wheelRadii,
				int[] wheelSpeed, boolean[] wheelIsMotor) {
			this.numVerts = numVerts;
			this.vertRadii = vertRadii;
			this.numWheels = numWheels;
			this.wheelVerts = wheelVerts;
			this.wheelRadii = wheelRadii;
			this.wheelSpeed = wheelSpeed;
			this.wheelIsMotor = wheelIsMotor;
		}

		public CarGene breed(CarGene mate) {
			int newNumVerts = (mate.numVerts + numVerts) / 2;
			int[] newVertRadii = new int[newNumVerts];

			for (int i = 0; i < newNumVerts; i++) {
				if (i == mate.numVerts)
					newVertRadii[i] = vertRadii[i];
				else if (i == numVerts)
					newVertRadii[i] = mate.vertRadii[i];
				else
					newVertRadii[i] = (mate.vertRadii[i] + vertRadii[i]) / 2;
			}

			int newNumWheels = (mate.numWheels + numWheels) / 2;

			if (newNumWheels > newNumVerts)
				newNumWheels = newNumVerts;

			int[] newWheelRadii = new int[newNumWheels];
			int[] newWheelSpeed = new int[newNumWheels];
			boolean[] newWheelIsMotor = new boolean[newNumWheels];

			for (int i = 0; i < newNumWheels; i++) {
				if (i == mate.numWheels) {
					newWheelRadii[i] = wheelRadii[i];
					newWheelSpeed[i] = wheelSpeed[i];
					newWheelIsMotor[i] = wheelIsMotor[i];
				} else if (i == numVerts) {
					newWheelRadii[i] = mate.wheelRadii[i];
					newWheelSpeed[i] = mate.wheelSpeed[i];
					newWheelIsMotor[i] = mate.wheelIsMotor[i];
				} else {
					newWheelRadii[i] = (mate.wheelRadii[i] + wheelRadii[i]) / 2;
					newWheelSpeed[i] = (mate.wheelSpeed[i] + wheelSpeed[i]) / 2;
					newWheelIsMotor[i] = mate.wheelIsMotor[i] || wheelIsMotor[i];
				}
			}

			int[] newWheelVerts = wheelVerts;

			if (rand.nextBoolean())
				newWheelVerts = mate.wheelVerts;

			return new CarGene(newNumVerts, newVertRadii, newNumWheels, newWheelVerts, newWheelRadii, newWheelSpeed,
					newWheelIsMotor);
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
			int[] vertRadii = new int[numVerts];

			for (int i = 0; i < numVerts; i++) {
				vertRadii[i] = randInt(vertRadiiMin, vertRadiiMax);
			}

			int numWheels = randInt(wheelNumMin, numVerts);

			ArrayList<Integer> wheelVertices = new ArrayList<Integer>();
			int vert;

			for (int i = 0; i < numWheels; i++) {
				do {
					vert = randInt(0, numVerts - 1);
				} while (wheelVertices.contains(vert));

				wheelVertices.add(vert);
			}

			int[] wheelVerts = new int[wheelVertices.size()];

			for (int i = 0, len = wheelVertices.size(); i < len; i++)
				wheelVerts[i] = wheelVertices.get(i);

			int[] wheelRadii = new int[numWheels];
			int[] wheelSpeed = new int[numWheels];
			boolean[] wheelIsMotor = new boolean[numWheels];

			for (int i = 0; i < numWheels; i++) {
				wheelRadii[i] = randInt(wheelRadiiMin, wheelRadiiMax);
				wheelSpeed[i] = randInt(wheelSpeedMin, wheelSpeedMax);
				wheelIsMotor[i] = rand.nextBoolean();
			}

			return new CarGene(numVerts, vertRadii, numWheels, wheelVerts, wheelRadii, wheelSpeed, wheelIsMotor);
		}
	}
}
