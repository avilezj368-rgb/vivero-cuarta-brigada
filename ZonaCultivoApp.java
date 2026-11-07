import java.io.*;
import java.util.Scanner;


public class ZonaCultivoApp {

    private static final String ARCHIVO_ZONAS = "zonas_cultivo.txt";
    private static final int MAX_ZONAS = 100;
    
    // Arreglo nativo y contador propio (Exigencia de la guía)
    private static String[] codigoZona = new String[MAX_ZONAS];
    private static String[] nombreZona = new String[MAX_ZONAS];
    private static String[] ubicacionZona = new String[MAX_ZONAS];
    private static double[] minUmbral = new double[MAX_ZONAS];
    private static double[] maxUmbral = new double[MAX_ZONAS];
    private static String[] estadoZona = new String[MAX_ZONAS];
    private static int contadorZonas = 0;

    // Pila propia implementada desde cero (Sin usar java.util.Stack)
    static class PilaZonas {
        private String[] registros;
        private int cima;
        private int capacidad;

        public PilaZonas(int capacidad) {
            this.capacidad = capacidad;
            this.registros = new String[capacidad];
            this.cima = -1; // -1 indica pila vacía
        }

        // Apila un registro eliminado
        public void apilar(String elemento) {
            if (cima < capacidad - 1) {
                registros[++cima] = elemento;
            }
        }

        // Desapila el último elemento eliminado
        public String desapilar() {
            if (estaVacia()) {
                return null; // Devuelve null si está vacía (exigencia de la guía)
            }
            return registros[cima--];
        }

        public boolean estaVacia() {
            return cima == -1;
        }
    }

    private static PilaZonas pilaEliminacion = new PilaZonas(MAX_ZONAS);

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        cargarDatosDesdeArchivo();

        int opcion = 0;
        do {
            System.out.println("\n==========================================================");
            System.out.println(" SISTEMA DE RIEGO - VIVERO BATALLÓN CUARTA BRIGADA (MÓDULO ZONAS)");
            System.out.println("==========================================================");
            System.out.println(" 1. Crear nueva zona de cultivo");
            System.out.println(" 2. Consultar zona por código exacto");
            System.out.println(" 3. Buscar zona por texto parcial (Nombre/Ubicación)");
            System.out.println(" 4. Actualizar umbrales o datos de zona");
            System.out.println(" 5. Eliminar zona de cultivo (Desplaza y apila)");
            System.out.println(" 6. Listar todas las zonas registradas");
            System.out.println(" 7. Deshacer última eliminación");
            System.out.println(" 8. Guardar y salir del sistema");
            System.out.println("----------------------------------------------------------");
            System.out.print("Seleccione una opción: ");

            if (scanner.hasNextInt()) {
                opcion = scanner.nextInt();
                scanner.nextLine(); // Limpiar buffer

                switch (opcion) {
                    case 1: crearZona(scanner); break;
                    case 2: consultarZonaPorCodigo(scanner); break;
                    case 3: buscarZonaPorTexto(scanner); break;
                    case 4: actualizarZona(scanner); break;
                    case 5: eliminarZona(scanner); break;
                    case 6: listarZonas(); break;
                    case 7: deshacerEliminacion(); break;
                    case 8: 
                        guardarDatosEnArchivo();
                        System.out.println("Datos guardados. Saliendo del sistema..."); 
                        break;
                    default: 
                        System.out.println("[ERROR] Opción no válida. Ingrese un número entre 1 y 8.");
                }
            } else {
                System.out.println("[ERROR] Debe ingresar un número entero válido.");
                scanner.next(); // Limpiar entrada errónea
            }
        } while (opcion != 8);

        scanner.close();
    }

    // ===========================================================
    // LAS 7 OPERACIONES OBLIGATORIAS EXIGIDAS POR LA GUÍA
    // ===========================================================

    // RF-01 · CA-01.1 / CA-01.2 — Registra zona validando código único y umbrales (0-100)
    private static void crearZona(Scanner scanner) {
        System.out.println("\n--- 1. CREAR NUEVA ZONA DE CULTIVO ---");
        if (contadorZonas >= MAX_ZONAS) {
            System.out.println("[ERROR] Capacidad máxima del arreglo alcanzada.");
            return;
        }

        System.out.print("Código de la zona (ej. ZN-01): ");
        String cod = scanner.nextLine().trim();

        // Validar código repetido recorriendo solo hasta el contador
        for (int i = 0; i < contadorZonas; i++) {
            if (codigoZona[i].equalsIgnoreCase(cod)) {
                System.out.println("[ERROR - CA-01.2] Ya existe una zona con el código: " + cod + ". Rechazado.");
                return;
            }
        }

        System.out.print("Nombre de la zona (ej. Germinación): ");
        String nom = scanner.nextLine().trim();
        System.out.print("Ubicación en el vivero: ");
        String ubi = scanner.nextLine().trim();

        System.out.print("Umbral mínimo de humedad (%): ");
        while (!scanner.hasNextDouble()) {
            System.out.print("[ERROR] Ingrese un valor numérico válido para el mínimo: ");
            scanner.next();
        }
        double min = scanner.nextDouble();

        System.out.print("Umbral máximo de humedad (%): ");
        while (!scanner.hasNextDouble()) {
            System.out.print("[ERROR] Ingrese un valor numérico válido para el máximo: ");
            scanner.next();
        }
        double max = scanner.nextDouble();
        scanner.nextLine();

        // RN-01: Validar rangos 0-100 y que min < max
        if (min < 0 || max > 100 || min >= max) {
            System.out.println("[ERROR - RN-01] Umbrales inválidos. Deben estar entre 0% y 100%, y el mínimo menor que el máximo.");
            return;
        }

        // Guardar en el arreglo en la posición del contador
        codigoZona[contadorZonas] = cod;
        nombreZona[contadorZonas] = nom;
        ubicacionZona[contadorZonas] = ubi;
        minUmbral[contadorZonas] = min;
        maxUmbral[contadorZonas] = max;
        estadoZona[contadorZonas] = "Activa";
        contadorZonas++;

        System.out.println("[ÉXITO - CA-01.1] Zona registrada correctamente.");
    }

    // RF-04 · Consulta por código exacto recorriendo hasta el contador
    private static void consultarZonaPorCodigo(Scanner scanner) {
        System.out.println("\n--- 2. CONSULTAR ZONA POR CÓDIGO ---");
        System.out.print("Ingrese el código a buscar: ");
        String cod = scanner.nextLine().trim();

        boolean encontrada = false;
        for (int i = 0; i < contadorZonas; i++) {
            if (codigoZona[i].equalsIgnoreCase(cod)) {
                System.out.println("----------------------------------------");
                System.out.println("Código: " + codigoZona[i]);
                System.out.println("Nombre: " + nombreZona[i]);
                System.out.println("Ubicación: " + ubicacionZona[i]);
                System.out.println("Umbrales: Mín " + minUmbral[i] + "% | Máx " + maxUmbral[i] + "%");
                System.out.println("Estado: " + estadoZona[i]);
                System.out.println("----------------------------------------");
                encontrada = true;
                break;
            }
        }
        if (!encontrada) {
            System.out.println("[AVISO] No se encontró ninguna zona con el código: " + cod);
        }
    }

    // RF-04 · Búsqueda por texto parcial en nombre o ubicación
    private static void buscarZonaPorTexto(Scanner scanner) {
        System.out.println("\n--- 3. BUSCAR ZONA POR TEXTO PARCIAL ---");
        System.out.print("Ingrese texto a buscar (nombre o ubicación): ");
        String texto = scanner.nextLine().trim().toLowerCase();

        boolean hayResultados = false;
        System.out.printf("%-8s | %-15s | %-15s | %-8s | %-8s | %-10s\n", "CÓDIGO", "NOMBRE", "UBICACIÓN", "MÍN (%)", "MÁX (%)", "ESTADO");
        System.out.println("----------------------------------------------------------------------------------");
        
        for (int i = 0; i < contadorZonas; i++) {
            if (nombreZona[i].toLowerCase().contains(texto) || ubicacionZona[i].toLowerCase().contains(texto)) {
                System.out.printf("%-8s | %-15s | %-15s | %-8.1f | %-8.1f | %-10s\n", 
                    codigoZona[i], nombreZona[i], ubicacionZona[i], minUmbral[i], maxUmbral[i], estadoZona[i]);
                hayResultados = true;
            }
        }
        if (!hayResultados) {
            System.out.println("[AVISO] Cero resultados encontrados para la búsqueda.");
        }
    }

    // RF-03 · Actualizar umbrales o datos de zona
    private static void actualizarZona(Scanner scanner) {
        System.out.println("\n--- 4. ACTUALIZAR ZONA DE CULTIVO ---");
        System.out.print("Ingrese el código de la zona a actualizar: ");
        String cod = scanner.nextLine().trim();

        int index = -1;
        for (int i = 0; i < contadorZonas; i++) {
            if (codigoZona[i].equalsIgnoreCase(cod)) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            System.out.println("[AVISO] Zona no encontrada.");
            return;
        }

        System.out.print("Nuevo nombre (Actual: " + nombreZona[index] + " - Enter para conservar): ");
        String nuevoNom = scanner.nextLine().trim();
        if (!nuevoNom.isEmpty()) nombreZona[index] = nuevoNom;

        System.out.print("Nuevo umbral mínimo (%): ");
        double nuevoMin = scanner.nextDouble();
        System.out.print("Nuevo umbral máximo (%): ");
        double nuevoMax = scanner.nextDouble();
        scanner.nextLine();

        if (nuevoMin < 0 || nuevoMax > 100 || nuevoMin >= nuevoMax) {
            System.out.println("[ERROR] Umbrales inválidos. No se aplicaron los cambios.");
            return;
        }

        minUmbral[index] = nuevoMin;
        maxUmbral[index] = nuevoMax;
        System.out.println("[ÉXITO] Zona actualizada correctamente.");
    }

    // RF-05 · Eliminar zona con desplazamiento estricto (Sin dejar huecos null) y apilado
    private static void eliminarZona(Scanner scanner) {
        System.out.println("\n--- 5. ELIMINAR ZONA DE CULTIVO ---");
        System.out.print("Ingrese el código de la zona a eliminar: ");
        String cod = scanner.nextLine().trim();

        int index = -1;
        for (int i = 0; i < contadorZonas; i++) {
            if (codigoZona[i].equalsIgnoreCase(cod)) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            System.out.println("[AVISO] Zona no encontrada para eliminar.");
            return;
        }

        // Serializar el registro para guardarlo en la Pila propia
        String registroEliminado = codigoZona[index] + ";" + nombreZona[index] + ";" + 
                                   ubicacionZona[index] + ";" + minUmbral[index] + ";" + 
                                   maxUmbral[index] + ";" + estadoZona[index];
        pilaEliminacion.apilar(registroEliminado);

        // Desplazar elementos a la izquierda (Exigencia clave de la guía)
        for (int i = index; i < contadorZonas - 1; i++) {
            codigoZona[i] = codigoZona[i + 1];
            nombreZona[i] = nombreZona[i + 1];
            ubicacionZona[i] = ubicacionZona[i + 1];
            minUmbral[i] = minUmbral[i + 1];
            maxUmbral[i] = maxUmbral[i + 1];
            estadoZona[i] = estadoZona[i + 1];
        }

        // Limpiar la última posición y decrementar contador
        contadorZonas--;
        codigoZona[contadorZonas] = null;
        nombreZona[contadorZonas] = null;
        ubicacionZona[contadorZonas] = null;

        System.out.println("[ÉXITO - CA-05.1] Zona eliminada, elementos desplazados y guardada en la Pila.");
    }

    // RNF-01 · Listar todas las zonas recorriendo únicamente hasta el contador
    private static void listarZonas() {
        System.out.println("\n--- 6. LISTAR ZONAS DE CULTIVO ---");
        if (contadorZonas == 0) {
            System.out.println("No hay zonas de cultivo registradas.");
            return;
        }

        System.out.printf("%-8s | %-15s | %-15s | %-8s | %-8s | %-10s\n", "CÓDIGO", "NOMBRE", "UBICACIÓN", "MÍN (%)", "MÁX (%)", "ESTADO");
        System.out.println("----------------------------------------------------------------------------------");
        for (int i = 0; i < contadorZonas; i++) {
            System.out.printf("%-8s | %-15s | %-15s | %-8.1f | %-8.1f | %-10s\n", 
                codigoZona[i], nombreZona[i], ubicacionZona[i], minUmbral[i], maxUmbral[i], estadoZona[i]);
        }
    }

    // CU-17 / Deshacer última eliminación usando la Pila propia
    private static void deshacerEliminacion() {
        System.out.println("\n--- 7. DESHACER ÚLTIMA ELIMINACIÓN ---");
        if (pilaEliminacion.estaVacia()) {
            System.out.println("[AVISO] La pila de eliminación está vacía. No hay nada que deshacer.");
            return;
        }

        if (contadorZonas >= MAX_ZONAS) {
            System.out.println("[ERROR] No se puede restaurar: el arreglo está lleno.");
            return;
        }

        String reg = pilaEliminacion.desapilar();
        String[] d = reg.split(";");

        // Restaurar al final del arreglo y subir el contador
        codigoZona[contadorZonas] = d[0];
        nombreZona[contadorZonas] = d[1];
        ubicacionZona[contadorZonas] = d[2];
        minUmbral[contadorZonas] = Double.parseDouble(d[3]);
        maxUmbral[contadorZonas] = Double.parseDouble(d[4]);
        estadoZona[contadorZonas] = d[5];
        contadorZonas++;

        System.out.println("[ÉXITO] Se restauró la zona eliminada con código: " + d[0]);
    }

    // ===========================================================
    // PERSISTENCIA EN ARCHIVO DE TEXTO PLANO (Cargar / Guardar)
    // ===========================================================

    private static void cargarDatosDesdeArchivo() {
        File archivo = new File(ARCHIVO_ZONAS);
        if (!archivo.exists()) {
            try {
                archivo.createNewFile();
            } catch (IOException e) {
                System.out.println("Error al crear el archivo de datos: " + e.getMessage());
            }
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null && contadorZonas < MAX_ZONAS) {
                if (!linea.trim().isEmpty()) {
                    String[] d = linea.split(";");
                    if (d.length >= 6) {
                        codigoZona[contadorZonas] = d[0];
                        nombreZona[contadorZonas] = d[1];
                        ubicacionZona[contadorZonas] = d[2];
                        minUmbral[contadorZonas] = Double.parseDouble(d[3]);
                        maxUmbral[contadorZonas] = Double.parseDouble(d[4]);
                        estadoZona[contadorZonas] = d[5];
                        contadorZonas++;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error al leer el archivo de persistencia: " + e.getMessage());
        }
    }

    private static void guardarDatosEnArchivo() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARCHIVO_ZONAS, false))) {
            for (int i = 0; i < contadorZonas; i++) {
                String linea = codigoZona[i] + ";" + nombreZona[i] + ";" + ubicacionZona[i] + ";" + 
                               minUmbral[i] + ";" + maxUmbral[i] + ";" + estadoZona[i];
                bw.write(linea);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error al guardar en el archivo de persistencia: " + e.getMessage());
        }
    }
}