package view;

public final class SessionContext {

    private static Integer currentPatientId;
    private static Integer currentMedecinId;

    private SessionContext() {
    }

    public static Integer getCurrentPatientId() {
        return currentPatientId;
    }

    public static void setCurrentPatientId(Integer patientId) {
        currentPatientId = patientId;
    }

    public static Integer getCurrentMedecinId() {
        return currentMedecinId;
    }

    public static void setCurrentMedecinId(Integer medecinId) {
        currentMedecinId = medecinId;
    }

    public static void clear() {
        currentPatientId = null;
        currentMedecinId = null;
    }
}
