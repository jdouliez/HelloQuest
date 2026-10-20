package fr.helloquest;

/** The phone activity and Android Auto service live in the same app process. */
final class PlaybackCoordinator {
    private static Runnable stopPhone;
    private static Runnable stopCar;
    private static boolean carActive;

    private PlaybackCoordinator() {}

    static void registerPhone(Runnable stop) { stopPhone=stop; }
    static void unregisterPhone(Runnable stop) { if(stopPhone==stop)stopPhone=null; }
    static void registerCar(Runnable stop) { stopCar=stop; }
    static void unregisterCar(Runnable stop) {
        if(stopCar==stop) {stopCar=null;carActive=false;}
    }

    static void startCar() {
        carActive=true;
        if(stopPhone!=null)stopPhone.run();
    }

    static void startPhone() {
        carActive=false;
        if(stopCar!=null)stopCar.run();
    }

    static boolean carActive() {return carActive;}
}
