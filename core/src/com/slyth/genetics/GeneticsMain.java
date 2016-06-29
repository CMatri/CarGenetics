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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

public class GeneticsMain extends ApplicationAdapter {
	SpriteBatch batch;
	ArrayList<Body> bars = new ArrayList<Body>();
	OrthographicCamera cam;
	World world = new World(new Vector2(0, -10), true);
	Box2DDebugRenderer debugRenderer;

	public Body genCarBody(int x, int y) {
		int numVertices = randInt(4, 10);
		
		Vector2[] offsets = { new Vector2(-10, 10), new Vector2(10, 10), new Vector2(10, -10), new Vector2(-10, -10) };//[numVertices];
		
		//for(Vector2 v : offsets) {
		//	v = new Vector2(randInt(-10, 10), randInt(-10, 10));
		//}
		
		PolygonShape shape = new PolygonShape();
		System.out.println(offsets);
		shape.set(offsets);
		
		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(new Vector2(x, y));
		Body groundBody = world.createBody(bodyDef);
		groundBody.createFixture(shape, 0.0f);
		
		return groundBody;
	}
	
	@Override
	public void create() {
		Box2D.init();
		batch = new SpriteBatch();
		debugRenderer = new Box2DDebugRenderer();

		cam = new OrthographicCamera(30, 30 * (Gdx.graphics.getHeight() / Gdx.graphics.getWidth()));
		cam.position.set(cam.viewportWidth / 2f, cam.viewportHeight / 2f, 0);
		cam.update();

		BodyDef groundBodyDef = new BodyDef();
		PolygonShape groundBox = new PolygonShape();

		for (int i = 0; i < 20; i++) {
			int x = randInt(0, 1920);
			int y = randInt(0, 1080);
			int width = randInt(10, 40);
			int height = 5;
			
			groundBodyDef.position.set(new Vector2(x, y));
			Body groundBody = world.createBody(groundBodyDef);
			groundBox.setAsBox(width, height);
			groundBody.createFixture(groundBox, 0.0f);

			bars.add(groundBody);
		}
		
		for(int i = 0; i < 10; i++) {
			genCarBody(50, 50);
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
		Random rand = new Random();
		int randomNum = rand.nextInt((max - min) + 1) + min;
		return randomNum;
	}

	private void handleInput() {
		int rotationSpeed = 2;

		if (Gdx.input.isKeyPressed(Input.Keys.A)) {
			cam.zoom += 0.2;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
			cam.zoom -= 0.2;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
			cam.translate(-9, 0, 0);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
			cam.translate(9, 0, 0);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
			cam.translate(0, -9, 0);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
			cam.translate(0, 9, 0);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.W)) {
			cam.rotate(-rotationSpeed, 0, 0, 1);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.E)) {
			cam.rotate(rotationSpeed, 0, 0, 1);
		}

		// cam.zoom = MathUtils.clamp(cam.zoom, 0.1f, 100 / cam.viewportWidth);

		float effectiveViewportWidth = cam.viewportWidth * cam.zoom;
		float effectiveViewportHeight = cam.viewportHeight * cam.zoom;

		// cam.position.x = MathUtils.clamp(cam.position.x,
		// effectiveViewportWidth / 2f,
		// 100 - effectiveViewportWidth / 2f);
		// cam.position.y = MathUtils.clamp(cam.position.y,
		// effectiveViewportHeight / 2f,
		// 100 - effectiveViewportHeight / 2f);
	}
}
