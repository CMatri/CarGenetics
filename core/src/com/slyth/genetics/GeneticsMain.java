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
    Vector2 beginPos;
    Vector2 vertex = Vector2.Zero;

    public Body createCarFromGene(CarGene gene, int x, int y) {
        ArrayList<Vector2> offsets = new ArrayList<Vector2>();
        ArrayList<PolygonShape> tris = new ArrayList<PolygonShape>();

        for (int i = 0; i < gene.vertRadii.size(); i++) {
            float radiusAtPoint = gene.vertRadii.get(i);
            float vx = (float) (radiusAtPoint * Math.cos(2 * Math.PI * i / gene.vertRadii.size()));
            float vy = (float) (radiusAtPoint * Math.sin(2 * Math.PI * i / gene.vertRadii.size()));
            offsets.add(new Vector2(vx, vy));
        }

        for (int i = 0; i < gene.vertRadii.size(); i++) {
            PolygonShape s = new PolygonShape();
            s.set(new Vector2[]{offsets.get(i), new Vector2(0, 0), i == gene.vertRadii.size() - 1 ? offsets.get(0) : offsets.get(i + 1)});
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
            axisDef.localAnchorA.set(offsets.get(gene.wheelVerts.get(i)));
            axisDef.localAnchorB.set(0, 0);
            axisDef.enableMotor = gene.wheelIsMotor.get(i);
            axisDef.maxMotorTorque = 200000;
            axisDef.motorSpeed = gene.wheelSpeed.get(i);
            RevoluteJoint axis = (RevoluteJoint) world.createJoint(axisDef);
        }

        gene.chassis = carBody;

        return carBody;
    }

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
            one = choices.get(Util.randInt(0, choices.size() - 1));
            do {
                two = choices.get(Util.randInt(0, choices.size() - 1));
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
            if (i > pastSize / 4) {
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
            int width = Util.randInt(0, 60);

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
            groundBodyDef.angle = i > numBars - 5 ? 0 : Util.randInt(-30 - factor, 30 + factor) * MathUtils.degreesToRadians;
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

    float done = 0;
    float doneMax = 100;

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
}
