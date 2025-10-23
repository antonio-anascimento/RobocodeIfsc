package roboifsc;

import robocode.*;
import java.awt.Color;

public class RobobatalhaIFSC extends AdvancedRobot {

    double energiaAnterior = 100;
    boolean andandoFrente = true;

    public void run() {
        setColors(Color.red, Color.blue, Color.blue);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true) {
            setTurnRadarRight(360);

            // Movimento imprevisível
            if (Math.random() < 0.05) {
                setTurnRight(90 - Math.random() * 180);
                andandoFrente = !andandoFrente;
            }

            evitarParede();
            setAhead(andandoFrente ? 100 : -100);

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

        double radarLock = getHeading() - getRadarHeading() + e.getBearing();
        setTurnRadarRight(radarLock);
        execute();
    }

    private void evitarParede() {
        double margem = 60;
        double x = getX();
        double y = getY();
        double largura = getBattleFieldWidth();
        double altura = getBattleFieldHeight();

        if (x < margem) {
            setTurnRight(0); // virar para a direita
            andandoFrente = true;
        } else if (x > largura - margem) {
            setTurnRight(180); // virar para a esquerda
            andandoFrente = true;
        }

        if (y < margem) {
            setTurnRight(90); // virar para cima
            andandoFrente = true;
        } else if (y > altura - margem) {
            setTurnRight(-90); // virar para baixo
            andandoFrente = true;
        }
    }
}
