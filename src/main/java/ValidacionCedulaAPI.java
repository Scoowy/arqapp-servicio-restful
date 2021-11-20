import spark.Request;
import spark.Response;
import spark.Spark;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RESTful para la validación de un número de CI de Ecuador.
 *
 * @author Juan Gahona
 * @version 2021.11.19.1442
 */
public class ValidacionCedulaAPI {
    public static void main(String[] args) {
        // Configuración del puerto de escucha
        Spark.port(80);

        // Endpoint a http://host/validar/
        Spark.get("/validar/", (req, res) -> """
                <p>Debe proporcionar un número de cédula en la URL</p>
                <p><code>http://[host:port]/validar/[cédula]</code></p>
                <p>Ejemplo: <a href="http://localhost/validar/0548912345">http://localhost/validar/0548912345</a></p>
                """);

        // Endpoint a http://host/validar/:ci
        Spark.get("/validar/:ci", ValidacionCedulaAPI::validationHandler);

    }

    /**
     * Manejador de la petición <code>[host:port]/validar/:ci</code>
     *
     * @param req Petición del cliente
     * @param res Respuesta del cliente
     * @return respuesta en JSON de la validación
     */
    private static String validationHandler(Request req, Response res) {
        // Se recupera el parámetro :ci de la URL
        String ci = req.params("ci");

        // Formato de la respuesta válida
        String validResponse = """
                {
                    "value": "%s",
                    "isValid": %b
                }
                """;

        // Formato de la respuesta inválida
        String invalidResponse = """
                {
                    "value": "%s",
                    "isValid": %b,
                    "errors": [ %s ]
                }
                """;

        // Variables de configuración
        int statusCode;
        String response;

        // Verificamos y obtenemos los posibles errores de la cédula
        List<String> errors = validateCI(ci);

        // Se comprueba si existen errores
        if (errors.isEmpty()) {
            // Si no hay errores la respuesta es válida
            response = String.format(validResponse, ci, true);
            statusCode = 200; // Se establece código 200 OK
        } else {
            // Si existen errores se añaden a la respuesta en JSON
            response = String.format(invalidResponse, ci, false, String.join(",", errors));
            statusCode = 400; // Se establece código 400 Bad Request
        }

        // Se configura el tipo de respuesta que se enviará al cliente
        res.type("application/json");
        // Se configura el código de estado
        res.status(statusCode);

        return response;
    }

    /**
     * Método que realiza la validación de una cédula
     * ecuatoriana, teniendo en cuenta:
     * <p>
     * - 10 caracteres de largo.
     * - Los dos primeros dígitos no sean mayor a 24.
     * - Todos los caracteres sean números.
     *
     * @param ci cédula de identidad
     * @return Una lista vacía si la cédula es correcta o los
     * errores que identifico.
     */
    private static List<String> validateCI(String ci) {
        // Lista de errores
        List<String> errors = new ArrayList<>();

        /*
        La cédula únicamente posee 10 dígitos.
         */
        if (ci.length() != 10) {
            errors.add("La CI debe ser de 10 caracteres de largo");
        }

        try {
            /*
             Extraemos y convertimos los dos primeros dígitos de la cédula,
             que conforman el código de provincia, este no puede ser
             mayor a 24.
            */
            int provinciaCode = Integer.parseInt(ci.substring(0, 2));

            if (provinciaCode > 24) {
                errors.add("Los dos primeros dígitos no deben ser un número mayor a 24");
            }

        } catch (NumberFormatException e) {
            System.out.printf("[ERROR] No se puede parsear %s a int%n", ci);
        } catch (IndexOutOfBoundsException e) {
            System.out.printf("[ERROR] Numero de dígitos en %s menor a 2%n", ci);
        }

        try {
            /*
             Se convierte a un número la cédula con el fin de comprobar
             que únicamente contenga caracteres numéricos, se convierte a
             Long por motivo de que int es de 32 bits lo cual permite un
             rango de números entre -2,147,483,648 y 2,147,483,647.
             Ejp: Al parsear la CI: 2147483658 se excede del rango de int.
            */
            Long.parseLong(ci);
        } catch (NumberFormatException e) {
            System.out.printf("[ERROR] No se puede parsear %s a Long%n", ci);
            errors.add("La cédula solo puede contener caracteres numéricos");
        }

        errors = errors.stream().map(error -> String.format("\"%s\"", error)).collect(Collectors.toList());

        return errors;
    }
}
