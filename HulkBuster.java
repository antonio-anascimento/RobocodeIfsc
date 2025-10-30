package roboifsc;

import robocode.*;
import java.awt.Color;
import java.awt.geom.Point2D;
import robocode.util.Utils;

public class HulkBuster extends AdvancedRobot {

    double energiaAnterior = 100;
    boolean andandoFrente = true;
    double margemParede = 60;

    static final double WALL_MARGIN = 18;

    @Override
    public void run() {
        setColors(new Color(170, 0, 0), Color.yellow, Color.yellow);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

        while (true) {
            if (estouPertoDaParede()) {
                evitarParede();
            } else {
                movimentoAleatorio();
            }
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        double quedaDeEnergia = energiaAnterior - e.getEnergy();
        energiaAnterior = e.getEnergy();
        if (quedaDeEnergia > 0 && quedaDeEnergia <= 3) {
            setTurnRight(e.getBearing() + (Math.random() > 0.5 ? 90 : -90));
            andandoFrente = !andandoFrente;
            setAhead(andandoFrente ? 100 : -100);
        }

        double absBearing = getHeadingRadians() + e.getBearingRadians();
        double radarTurn = Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians());
        setTurnRadarRightRadians(radarTurn * 2);

        Point2D.Double minhaPos = new Point2D.Double(getX(), getY());

        double inimigoX = minhaPos.x + e.getDistance() * Math.sin(absBearing);
        double inimigoY = minhaPos.y + e.getDistance() * Math.cos(absBearing);

        double bulletPower = escolherPotenciaTiro(e);
        double bulletSpeed = 20 - 3 * bulletPower; 

        double inimigoHeading = e.getHeadingRadians();
        double inimigoVel = e.getVelocity();

        double t = 0.0;
        double px = inimigoX;
        double py = inimigoY;

        for (int i = 0; i < 20; i++) {
            double dist = minhaPos.distance(px, py);
            double newT = dist / bulletSpeed;

            if (Math.abs(newT - t) < 0.1) {
                t = newT;
                break;
            }
            t = newT;
            px = inimigoX + inimigoVel * t * Math.sin(inimigoHeading);
            py = inimigoY + inimigoVel * t * Math.cos(inimigoHeading);

            px = clamp(px, WALL_MARGIN, getBattleFieldWidth() - WALL_MARGIN);
            py = clamp(py, WALL_MARGIN, getBattleFieldHeight() - WALL_MARGIN);
        }

        double anguloParaPredicao = Math.atan2(px - minhaPos.x, py - minhaPos.y);
        double gunTurn = Utils.normalRelativeAngle(anguloParaPredicao - getGunHeadingRadians());
        setTurnGunRightRadians(gunTurn);

        
        if (getGunHeat() == 0 && Math.abs(gunTurn) < Math.toRadians(20)) {
            setFire(bulletPower);
        }
    }

    private double escolherPotenciaTiro(ScannedRobotEvent e) {
        double dist = e.getDistance();
        double power;
        if (dist < 150)       power = 3.0;   
        else if (dist < 300)  power = 2.4;
        else if (dist < 500)  power = 1.8;
        else                  power = 1.3;   

        if (getEnergy() < 15) power = Math.min(power, 1.2);

        power = Math.min(power, Math.max(0.1, e.getEnergy() / 3.0));

        return power;
    }

    private boolean estouPertoDaParede() {
        return (getX() < margemParede
                || getX() > getBattleFieldWidth() - margemParede
                || getY() < margemParede
                || getY() > getBattleFieldHeight() - margemParede);
    }

    private void movimentoAleatorio() {

        if (Math.random() < 0.05) {
            setTurnRight(90 - Math.random() * 180);
            andandoFrente = !andandoFrente;
        }
        setAhead(andandoFrente ? 100 : -100);
    }

    private void evitarParede() {
        double x = getX();
        double y = getY();

        double anguloParaCentro = Math.atan2((getBattleFieldWidth() / 2.0) - x,
                                             (getBattleFieldHeight() / 2.0) - y);

        double turnAngleDeg = Math.toDegrees(anguloParaCentro) - getHeading();
        double turnAngle = Utils.normalRelativeAngleDegrees(turnAngleDeg);

        setTurnRight(turnAngle);
        andandoFrente = true;
        setAhead(120);
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        setBack(150);
        setTurnRight(90);
        andandoFrente = true;
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {

        setBack(50);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}