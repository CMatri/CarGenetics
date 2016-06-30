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
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
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
	OrthographicCamera cam;
	World world = new World(new Vector2(0, -10), true);
	Box2DDebugRenderer debugRenderer;
	static Random rand = new Random();

	public Body genCarBody(int x, int y) {
		int numVertices = randInt(4, 8);
		Vector2[] offsets = new Vector2[numVertices];

		for (int i = 0; i < numVertices; i++) {
			float radiusAtPoint = rand.nextFloat() * 20;
			float vx = (float) (radiusAtPoint * Math.cos(2 * Math.PI * i / numVertices));
			float vy = (float) (radiusAtPoint * Math.sin(2 * Math.PI * i / numVertices));
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
		Body carBody = world.createBody(bodyDef);
		carBody.createFixture(shape, 10.0f);
		carBody.getFixtureList().get(0).setFilterData(f);

		CircleShape wheelShape = new CircleShape();
		FixtureDef wheelFixtureDef = new FixtureDef();
		wheelFixtureDef.density = 100;

		int numWheels = randInt(1, numVertices);
		ArrayList<Integer> wheelVertices = new ArrayList<Integer>();
		int vert;

		for (int i = 0; i < numWheels; i++) {
			do {
				vert = randInt(0, numVertices - 1);
			} while (wheelVertices.contains(vert));

			wheelVertices.add(vert);
		}

		for (Integer i : wheelVertices) {
			wheelShape.setRadius(randInt(1, 5));
			wheelFixtureDef.shape = wheelShape;
			wheelFixtureDef.filter.categoryBits = f.categoryBits;
			wheelFixtureDef.filter.maskBits = f.maskBits;
			Body wheelBody = world.createBody(bodyDef);
			wheelBody.createFixture(wheelFixtureDef);

			RevoluteJointDef axisDef = new RevoluteJointDef();
			axisDef.bodyA = carBody;
			axisDef.bodyB = wheelBody;
			axisDef.collideConnected = false;
			axisDef.localAnchorA.set(offsets[i]);
			axisDef.localAnchorB.set(0, 0);
			axisDef.enableMotor = true;
			axisDef.maxMotorTorque = 20000;
			axisDef.motorSpeed = 20.0f;
			RevoluteJoint axis = (RevoluteJoint) world.createJoint(axisDef);
		}

		return carBody;
	}

	@Override
	public void create() {
		Box2D.init();
		batch = new SpriteBatch();
		debugRenderer = new Box2DDebugRenderer();

		cam = new OrthographicCamera(30, 30 * (Gdx.graphics.getHeight() / Gdx.graphics.getWidth()));
		cam.position.set(cam.viewportWidth / 4f, cam.viewportHeight / 4f, 0);
		cam.update();

		BodyDef groundBodyDef = new BodyDef();
		PolygonShape groundBox = new PolygonShape();

		for (int i = 0; i < 20; i++) {
			int width = 400;//randInt(10, 40);
			int height = 5;
			int x = i * width; // randInt(0, 1920);
			int y = 0;// randInt(0, 1080);

			groundBodyDef.position.set(new Vector2(x, y));
			Body groundBody = world.createBody(groundBodyDef);
			groundBox.setAsBox(width, height);
			groundBody.createFixture(groundBox, 0.0f);

			bars.add(groundBody);
		}

		for (int i = 0; i < 30; i++) {
			genCarBody(randInt(0, 700), 40);
		}

		groundBox.dispose();
	}

	@Override
	public void resize(int width, int height) {
		cam.viewportWidth = 30f;
		cam.viewportHeight = 30f * height / width;
		cam.update();
	}

	@Override
	public void render() {
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
		int rotationSpeed = 2;

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
	}
}
