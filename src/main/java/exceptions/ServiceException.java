package exceptions;

/**
 * Erreur métier ou de persistance exposée aux contrôleurs.
 */
public class ServiceException extends Exception {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Message + causes JDBC / SQL pour affichage dans l’interface.
     */
    public String formatWithCauses() {
        StringBuilder sb = new StringBuilder(getMessage() != null ? getMessage() : "Erreur");
        Throwable t = getCause();
        int depth = 0;
        while (t != null && depth++ < 8) {
            String m = t.getMessage();
            sb.append("\n› ");
            sb.append(m != null && !m.isBlank() ? m : t.getClass().getSimpleName());
            t = t.getCause();
        }
        return sb.toString();
    }
}
