package view;

final class SessionContext {

    private static Integer currentPatientId;
    private static Integer currentMedecinId;

    private SessionContext() {
    }

    static Integer getCurrentPatientId() {
        return currentPatientId;
    }

    static void setCurrentPatientId(Integer patientId) {
        currentPatientId = patientId;
    }

    static Integer getCurrentMedecinId() {
        return currentMedecinId;
    }

    static void setCurrentMedecinId(Integer medecinId) {
        currentMedecinId = medecinId;
    }

    static void clear() {
        currentPatientId = null;
        currentMedecinId = null;
    }
}
