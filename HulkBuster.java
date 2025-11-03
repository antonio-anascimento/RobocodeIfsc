package roboifsc;

import robocode.*;
import java.awt.Color;
import java.awt.geom.Point2D;
import robocode.util.Utils;
import java.awt.Graphics2D;
import java.awt.BasicStroke;

public class HulkBuster extends AdvancedRobot {

    double energiaAnterior = 100;
    boolean andandoFrente = true;
    double margemParede = 40;

    static final double WALL_MARGIN = 40;
    static final double MAX_PREDICTION_TIME = 2.5; //Para limitar o tempo de predição para não "viajar" demais.

    //Laser: guarda a potência do último disparo para definir a cor do feixe de luz.
    double ultimoBulletPower = 0;

    //Laser: guarda a última distância até o inimido escaneado
    double ultimaDistanciaInimigo = 0;
    
    @Override
    public void run() {
        setColors(new Color(170, 0, 0), Color.yellow, Color.yellow);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        // Este comando é a sua "busca padrão" do radar
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
            setAhead(andandoFrente ? 120 : -120);
        }

        double absBearing = getHeadingRadians() + e.getBearingRadians();
        double radarTurn = Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians());
        
        // Este comando é a sua "trava" de radar. Ele SOBRESCREVE o giro infinito do run()
        setTurnRadarRightRadians(radarTurn * 2);

        Point2D.Double minhaPos = new Point2D.Double(getX(), getY());

        double inimigoX = minhaPos.x + e.getDistance() * Math.sin(absBearing);
        double inimigoY = minhaPos.y + e.getDistance() * Math.cos(absBearing);

        ultimaDistanciaInimigo = e.getDistance(); //Grava a distância até o inimigo para ajustar o feixe de laser.
        
        double bulletPower = escolherPotenciaTiro(e);
        if (bulletPower == 0) { //em situações de energia muito baixa e inimigo longe, decide não atirar.

            return;
        }

        //Laser: guarda a potência atual para desenhar o laser com a cor correspondente.
        ultimoBulletPower = bulletPower;

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

        
        if (getGunHeat() <= 0 && Math.abs(gunTurn) < Math.toRadians(20) && getEnergy() > 0.5) {
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

    // --- CORREÇÃO 1: REMOVIDO CONFLITO DE MOVIMENTO ---
    @Override
    public void onHitWall(HitWallEvent e) {
        // VAZIO.
        // Deixar vazio evita conflito com a lógica "evitarParede()" do run().
        // Isso impede que o robô trave na parede.
    }

    // --- CORREÇÃO 2: LÓGICA DE AMEAÇA NO RADAR ---
    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        
        // Só quebra a trava do radar se o tiro for FORTE (potência > 2.0)
        // Isso sugere que o atirador está perto e é uma ameaça real.
        if (e.getPower() > 2.0) {
            // Quebra a trava e reativa a busca infinita do radar
            setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
        // Se o tiro for fraco (<= 2.0), ele é ignorado e o robô
        // continua focado no alvo original.

        // O comando "setBack(50)" foi removido para evitar conflito de movimento.
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
    
    // --- CORREÇÃO 3: EVITAR TRAVA EM ROBÔ MORTO ---
    @Override
    public void onRobotDeath(RobotDeathEvent e) {
            // Quebra a trava do radar e reativa a busca infinita
            // para procurar o próximo alvo.
            setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
        }

        //Laser - feixe de luz duplo piscando + comprimento pela distância
        //*aparece somente quando o canhão está pronto (gunHeat<=0)
        //*pisca conforme getTime()
        //*cor depende do ultimoBulletPower
        //*comprimento depende de ultimaDistanciaInimigo

        @Override
        public void onPaint(Graphics2D g) {
        //Se o canhão ainda está esquentando, não desenha o laser
        if (getGunHeat() > 0) {
            return;
        }

        //Efeito pisca-pisca: metade do tempo não desenha nada
        //Ajuste para deixar mais rápido (8 menor) ou mais lento (8 maior)
        long t = getTime();
        if (t % 8 < 4) {
            return;
        }

        //Cor do feixe conforme potência do último tiro
        if (ultimoBulletPower >= 2.5) {
            g.setColor(Color.RED);           //tiros fortes = vermelho
        } else if (ultimoBulletPower >= 1.5) {
            g.setColor(Color.ORANGE);        //tiros médios = laranja
        } else if (ultimoBulletPower > 0) {
            g.setColor(Color.GREEN);         //tiros fracos = verde
        } else {
            g.setColor(Color.GRAY);          //ainda não atirou
        }

        double x = getX();
        double y = getY();

        //Definindo intervalo de distâncias
        double minDist = 100; //muito perto
        double maxDist = 800; //bem longe

        //Intervalo de comprimento do feixe
        double minLen = 150;
        double maxLen = 1000;

        double d = ultimaDistanciaInimigo;

        //Se ainda não tem distância inicial, usa valor intermediário
        if (d <=0) {
            d = (minDist + maxDist) / 2.0;
        }

        //Clampa a distância no intervalo min e max
        d = Math.max(minDist, Math.min(maxDist, d));

        //Normaliza para [0, 1]
        double norm = (d - minDist) / (maxDist - minDist);

        // Calcula comprimento principal: perto -> menor, longe -> maior
        double comprimentoFeixePrincipal  = minLen + norm * (maxLen - minLen);
        double comprimentoFeixeSecundario = comprimentoFeixePrincipal * 0.7; // secundário um pouco menor

        //Fim do feixe principal
        double endX1 = x + Math.sin(getGunHeadingRadians()) * comprimentoFeixePrincipal;
        double endY1 = y + Math.cos(getGunHeadingRadians()) * comprimentoFeixePrincipal;

        //Fim do feixe secundário
        double endX2 = x + Math.sin(getGunHeadingRadians()) * comprimentoFeixeSecundario;
        double endY2 = y + Math.cos(getGunHeadingRadians()) * comprimentoFeixeSecundario;

        //Feixe principal: mais grosso
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine((int) x, (int) y, (int) endX1, (int) endY1);

        //Feixe secundário: mais fino e com brilho
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(255, 255, 150)); // amarelo claro
        g.drawLine((int) x, (int) y, (int) endX2, (int) endY2);
    }
}