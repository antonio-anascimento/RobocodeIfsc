package roboifsc;

import robocode.*;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import robocode.util.Utils;
import java.awt.Graphics2D;
import java.awt.BasicStroke;

public class HulkBuster extends AdvancedRobot {

    // Informações de cada inimigo
    static class EnemyInfo {
        double x;
        double y;
        double distance;
        double energy;
        double heading;
        double velocity;
        long lastSeenTime;
    }

    private final Map<String, EnemyInfo> enemies = new HashMap<>();
    private int moveDirection = 1;
    private static final double WALL_MARGIN = 40;

    // Laser
    private double ultimoBulletPower = 0;
    private double ultimaDistanciaInimigo = 0;

    @Override
    public void run() {
        setColors(new Color(170, 0, 0), Color.YELLOW, Color.YELLOW);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        // Radar gira infinito por padrão
        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

        while (true) {
            fazerMovimento();
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        double myX = getX();
        double myY = getY();
        double absBearing = getHeadingRadians() + e.getBearingRadians();

        // Trava radar no inimigo escaneado
        double radarTurn = Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians());
        setTurnRadarRightRadians(radarTurn * 2);

        double inimigoX = myX + e.getDistance() * Math.sin(absBearing);
        double inimigoY = myY + e.getDistance() * Math.cos(absBearing);

        EnemyInfo enemy = enemies.get(e.getName());
        if (enemy == null) {
            enemy = new EnemyInfo();
        }

        // Queda de energia do inimigo → provável disparo
        double deltaEnergy = (enemy.energy == 0 ? 0 : enemy.energy - e.getEnergy());

        enemy.energy   = e.getEnergy();
        enemy.distance = e.getDistance();
        enemy.x        = inimigoX;
        enemy.y        = inimigoY;
        enemy.heading  = e.getHeadingRadians();
        enemy.velocity = e.getVelocity();
        enemy.lastSeenTime = getTime();
        enemies.put(e.getName(), enemy);

        // Se o inimigo atirou, inverte direção pra desviar
        if (deltaEnergy > 0 && deltaEnergy <= 3) {
            moveDirection = -moveDirection;
        }

        ultimaDistanciaInimigo = e.getDistance();

        boolean melee = getOthers() > 2;
        double bulletPower = escolherPotenciaTiro(e, melee);
        if (bulletPower <= 0 || getEnergy() <= 0.5) {
            return;
        }

        ultimoBulletPower = bulletPower;

        double gunTurn;
        // Em melee, se estiver bem perto, mira direto (rápido)
        if (melee && e.getDistance() < 200) {
            gunTurn = Utils.normalRelativeAngle(absBearing - getGunHeadingRadians());
        } else {
            gunTurn = calcularGunTurnComPredicao(enemy, bulletPower);
        }

        setTurnGunRightRadians(gunTurn);

        // Só dispara se estiver relativamente alinhado
        if (getGunHeat() == 0 && Math.abs(gunTurn) < Math.toRadians(8)) {
            setFire(bulletPower);
        }
    }

    private double calcularGunTurnComPredicao(EnemyInfo enemy, double bulletPower) {
        double bulletSpeed = 20 - 3 * bulletPower;
        double myX = getX();
        double myY = getY();

        double ex = enemy.x;
        double ey = enemy.y;
        double eh = enemy.heading;
        double ev = enemy.velocity;

        double px = ex;
        double py = ey;
        double t = 0.0;

        for (int i = 0; i < 20; i++) {
            double dist = Point2D.distance(myX, myY, px, py);
            double newT = dist / bulletSpeed;

            if (Math.abs(newT - t) < 0.1) {
                t = newT;
                break;
            }

            t = newT;

            px = ex + ev * t * Math.sin(eh);
            py = ey + ev * t * Math.cos(eh);

            px = clamp(px, WALL_MARGIN, getBattleFieldWidth()  - WALL_MARGIN);
            py = clamp(py, WALL_MARGIN, getBattleFieldHeight() - WALL_MARGIN);
        }

        double anguloPred = Math.atan2(px - myX, py - myY);
        return Utils.normalRelativeAngle(anguloPred - getGunHeadingRadians());
    }

    private double escolherPotenciaTiro(ScannedRobotEvent e, boolean melee) {
        double dist = e.getDistance();
        double power;

        // Base na distância
        if (dist < 150 && getEnergy() > 30) {
            power = 3.0;
        } else if (dist < 300) {
            power = 2.5;
        } else if (dist < 500) {
            power = 2.0;
        } else {
            power = 1.5;
        }

        // Economia extra quando a energia começa a ficar perigosa
        if (getEnergy() < 25) {
            power = Math.min(power, 1.8);
        }
        if (getEnergy() < 15) {
            power = Math.min(power, 1.2);
        }

        // Em melee com muitos robôs, ainda mais conservador
        if (melee && getOthers() > 5) {
            power = Math.min(power, 2.0);
        }

        // Não atira com muito mais força do que o inimigo suporta
        power = Math.min(power, Math.max(0.1, e.getEnergy() / 3.0));

        return power;
    }

    // ---------------- MOVIMENTO (dispatcher) ----------------

    private void fazerMovimento() {
        if (getOthers() <= 2) {
            // Duelo / poucos inimigos → movimento orbital (mantido)
            fazerMovimentoDuel();
        } else {
            // Arena cheia → anti-gravidade defensivo
            fazerMovimentoAntiGravidade();
        }
    }

    private EnemyInfo getClosestEnemy() {
        double bestDist = Double.MAX_VALUE;
        EnemyInfo best = null;
        long now = getTime();

        for (EnemyInfo e : enemies.values()) {
            if (now - e.lastSeenTime > 60) continue;
            if (e.distance < bestDist) {
                bestDist = e.distance;
                best = e;
            }
        }
        return best;
    }

    // --------- MOVIMENTO DE DUELO (1x1) – mantido ---------
    private void fazerMovimentoDuel() {
        EnemyInfo target = getClosestEnemy();
        if (target == null) {
            // fallback se algo der errado
            fazerMovimentoAntiGravidade();
            return;
        }

        double x = getX();
        double y = getY();

        double dx = target.x - x;
        double dy = target.y - y;
        double dist = Math.hypot(dx, dy);

        double angleToEnemy = Math.atan2(dx, dy);

        // Base: orbita perpendicular ao inimigo
        double moveAngle = angleToEnemy + (Math.PI / 2.0) * moveDirection;

        // Ajuste de distância
        if (dist < 200) {
            moveAngle = angleToEnemy + Math.PI;      // foge se muito perto
        } else if (dist > 500) {
            moveAngle = angleToEnemy;                // aproxima se muito longe
        }

        if (Math.random() < 0.05) {
            moveDirection = -moveDirection;
        }

        double turn = Utils.normalRelativeAngle(moveAngle - getHeadingRadians());
        setTurnRightRadians(turn);
        setAhead(140 * moveDirection);
    }

    // --------- MOVIMENTO ANTI-GRAVIDADE – focado em sobrevivência ---------
    private void fazerMovimentoAntiGravidade() {
        double x = getX();
        double y = getY();
        double bfWidth = getBattleFieldWidth();
        double bfHeight = getBattleFieldHeight();
        long now = getTime();

        // Se muito perto da parede, prioriza sair dela
        if (estouPertoDaParede()) {
            double anguloParaCentro = Math.atan2(
                (bfWidth / 2.0) - x,
                (bfHeight / 2.0) - y
            );
            double turnAngle = Utils.normalRelativeAngle(anguloParaCentro - getHeadingRadians());
            setTurnRightRadians(turnAngle);
            setAhead(100 * moveDirection);   // passo menor = mais controle
            return;
        }

        double forceX = 0;
        double forceY = 0;

        // Repulsão de inimigos
        for (EnemyInfo enemy : enemies.values()) {
            if (now - enemy.lastSeenTime > 60) continue;

            double dx = x - enemy.x;
            double dy = y - enemy.y;
            double dist2 = dx * dx + dy * dy;
            if (dist2 < 1) dist2 = 1;

            double dist = Math.sqrt(dist2);

            double force = 50000 / dist2;

            // Inimigo muito perto gera repulsão extra (sobrevivência)
            if (dist < 200) {
                force *= 3.0;
            } else if (dist < 350) {
                force *= 1.5;
            }

            forceX += force * (dx / dist);
            forceY += force * (dy / dist);
        }

        // Repulsão das paredes
        double dLeft   = Math.max(1, x - WALL_MARGIN);
        double dRight  = Math.max(1, bfWidth  - x - WALL_MARGIN);
        double dBottom = Math.max(1, y - WALL_MARGIN);
        double dTop    = Math.max(1, bfHeight - y - WALL_MARGIN);

        double wallForce = 130000; // um pouco mais forte

        forceX +=  wallForce / (dLeft  * dLeft);
        forceX -=  wallForce / (dRight * dRight);
        forceY +=  wallForce / (dBottom * dBottom);
        forceY -=  wallForce / (dTop    * dTop);

        // Pequena atração para o centro para evitar ficar encurralado
        double centerX = bfWidth / 2.0;
        double centerY = bfHeight / 2.0;
        double dcx = centerX - x;
        double dcy = centerY - y;
        double distCenter2 = dcx * dcx + dcy * dcy;
        if (distCenter2 > 1) {
            double distCenter = Math.sqrt(distCenter2);
            double centerForce = 18000 / distCenter2; // fraquinho, só corrige rota
            forceX += centerForce * (dcx / distCenter);
            forceY += centerForce * (dcy / distCenter);
        }

        // Pequena aleatoriedade na direção
        if (Math.random() < 0.02) {
            moveDirection = -moveDirection;
        }

        // Se o vetor de força ficou muito pequeno, movimento padrão
        if (Math.abs(forceX) < 0.001 && Math.abs(forceY) < 0.001) {
            setTurnRight(10);
            setAhead(70 * moveDirection);
            return;
        }

        double moveAngle = Math.atan2(forceX, forceY);
        double turn = Utils.normalRelativeAngle(moveAngle - getHeadingRadians());
        setTurnRightRadians(turn);
        setAhead(100 * moveDirection);   // passo mais curto em melee
    }

    private boolean estouPertoDaParede() {
        return (getX() < WALL_MARGIN
                || getX() > getBattleFieldWidth() - WALL_MARGIN
                || getY() < WALL_MARGIN
                || getY() > getBattleFieldHeight() - WALL_MARGIN);
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        moveDirection = -moveDirection;
        setTurnRight(90);
        setAhead(150 * moveDirection);
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        moveDirection = -moveDirection;
        if (e.getPower() > 2.0) {
            setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        enemies.remove(e.getName());
        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // --------------------- LASER GRÁFICO ---------------------

    @Override
    public void onPaint(Graphics2D g) {
        if (getGunHeat() > 0) {
            return;
        }

        long t = getTime();
        if (t % 8 < 4) {
            return;
        }

        if (ultimoBulletPower >= 2.5) {
            g.setColor(Color.RED);
        } else if (ultimoBulletPower >= 1.5) {
            g.setColor(Color.ORANGE);
        } else if (ultimoBulletPower > 0) {
            g.setColor(Color.GREEN);
        } else {
            g.setColor(Color.GRAY);
        }

        double x = getX();
        double y = getY();

        double minDist = 100;
        double maxDist = 800;
        double minLen  = 150;
        double maxLen  = 1000;

        double d = ultimaDistanciaInimigo;
        if (d <= 0) {
            d = (minDist + maxDist) / 2.0;
        }

        d = Math.max(minDist, Math.min(maxDist, d));
        double norm = (d - minDist) / (maxDist - minDist);

        double comprimentoFeixePrincipal  = minLen + norm * (maxLen - minLen);
        double comprimentoFeixeSecundario = comprimentoFeixePrincipal * 0.7;

        double endX1 = x + Math.sin(getGunHeadingRadians()) * comprimentoFeixePrincipal;
        double endY1 = y + Math.cos(getGunHeadingRadians()) * comprimentoFeixePrincipal;

        double endX2 = x + Math.sin(getGunHeadingRadians()) * comprimentoFeixeSecundario;
        double endY2 = y + Math.cos(getGunHeadingRadians()) * comprimentoFeixeSecundario;

        g.setStroke(new BasicStroke(2.5f));
        g.drawLine((int) x, (int) y, (int) endX1, (int) endY1);

        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(255, 255, 150));
        g.drawLine((int) x, (int) y, (int) endX2, (int) endY2);
    }
}
