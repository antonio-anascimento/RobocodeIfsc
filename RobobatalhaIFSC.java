package roboifsc;

import robocode.*;
import java.awt.Color;
import robocode.util.Utils; 

public class RobobatalhaIFSC extends AdvancedRobot {

    double energiaAnterior = 100;
    boolean andandoFrente = true;
    double margemParede = 60; 

    public void run() {
        setColors(Color.red, Color.blue, Color.blue);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

   
        setTurnRadarRight(Double.POSITIVE_INFINITY); 

        while (true) {
            
            if (estouPertoDaParede()) {
                evitarParede();
            } else {
                movimentoAleatorio();
            }

            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {

        double quedaDeEnergia = energiaAnterior - e.getEnergy();
        energiaAnterior = e.getEnergy();

        if (quedaDeEnergia > 0 && quedaDeEnergia <= 3) {
            setTurnRight(e.getBearing() + (Math.random() > 0.5 ? 90 : -90));
            andandoFrente = !andandoFrente;
        }

        double absoluteBearing = getHeading() + e.getBearing();
        double radarTurn = Utils.normalRelativeAngleDegrees(absoluteBearing - getRadarHeading());
        
        setTurnRadarRight(radarTurn);

        setFire(2);

    private boolean estouPertoDaParede() {
        return (getX() < margemParede || getX() > getBattleFieldWidth() - margemParede ||
                getY() < margemParede || getY() > getBattleFieldHeight() - margemParede);
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
        
        double anguloParaCentro = Math.atan2(getBattleFieldWidth() / 2 - x, getBattleFieldHeight() / 2 - y);
        

        double turnAngle = Utils.normalRelativeAngleDegrees(Math.toDegrees(anguloParaCentro) - getHeading());

        setTurnRight(turnAngle);
        andandoFrente = true;
        setAhead(100);
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
}
