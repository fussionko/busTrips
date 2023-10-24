import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.Reference;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class busTrips {
    private static final int ARG_STOP_ID = 0;
    private static final int ARG_NUMBER_OF_BUSES = 1;
    private static final int ARG_DATA_TYPE = 2;

    private static final String FILE_PATH_MAIN = "./gtfs";
    private static final String FILE_PATH_STOPS = FILE_PATH_MAIN + "/stops.txt";
    private static final String FILE_PATH_STOP_TIMES = FILE_PATH_MAIN + "/stop_times.txt";
    private static final String FILE_PATH_TRIPS = FILE_PATH_MAIN + "/trips.txt";
    private static final String FILE_PATH_ROUTES = FILE_PATH_MAIN + "/routes.txt";

    private static enum DataType {
        RELATIVE, ABSOLUTE
    };  

    private static final String DATA_TYPE_RELATIVE = "relative";
    private static final String DATA_TYPE_ABSOLUTE = "absolute";

    private static final int DISPLAY_TIME_MINUTES = 120;

    private static final int ABSOLUTE_MAX_BUSES = 5;
    private static final int RELATIVE_MAX_BUSES = 3;

    private static final long dayInSeconds = 24 * 60 * 60;

    // Preveri ce je String Integer
    private static boolean isPositiveInteger(String str) {
        try { 
            int tmp = Integer.parseInt(str); 
            if (tmp < 0) return false;
        } catch(Exception e) { 
            return false; 
        } 

  
        return true;
    }

    private static long diffTimeInSeconds(final LocalTime ld1, final LocalTime ld2) {
        // Izracunaj razliko v casu -> ce je negativna razlika je slo cez potem pristej dan
        long timeDiff = ld1.until(ld2, ChronoUnit.SECONDS);
        if (timeDiff < 0) {
            timeDiff += dayInSeconds;
        } 

        return timeDiff;
    }

    private static boolean stopExists(final String stopId) {
        try {
            File stopsFile = new File(FILE_PATH_STOPS);
            Scanner stopsReader = new Scanner(stopsFile).useDelimiter(",");

            // Spusti opis podatkov
            stopsReader.nextLine();
   
            while (stopsReader.hasNextLine()) {
                String readStopId = stopsReader.next();

                // Stop najden
                if (stopId.compareTo(readStopId) == 0)
                {
                    stopsReader.close();
                    return true;
                }

                stopsReader.nextLine();
            }
            stopsReader.close();
        } catch (FileNotFoundException e) {
            return false;
        }

        // Noben ni najden
        return false;
    }

    // Vrne ime postaje glede na id - ce postaja neobstaja vrne null String
    private static String getStopName(final String stopId) {
        final String fieldName = "stop_name";
        try {
            File stopsFile = new File(FILE_PATH_STOPS);
            Scanner stopsReader = new Scanner(stopsFile).useDelimiter(",");

            // Pridobi kateri povrsti je stop_name podatek
            boolean skipData = false;
            // Spusti stop_id
            stopsReader.next();
            if (fieldName.compareTo(stopsReader.next()) != 0) {
                skipData = true;
            } 
            // Spusti opis podatkov
            stopsReader.nextLine();
   
            while (stopsReader.hasNextLine()) {
                String readStopId = stopsReader.next();

                // Stop ime najden
                if (stopId.compareTo(readStopId) == 0)
                {
                    // Spusti nepomembne podatke
                    if (skipData == true)  stopsReader.next();

                    String dataName = stopsReader.next();
                    
                    stopsReader.close();
                    return dataName;
                }

                stopsReader.nextLine();
            }
            stopsReader.close();
        } catch (FileNotFoundException e) {
            return null;
        }

        // Noben ni najden
        return null;
    }

    private static Map<String, List<LocalDateTime>> getTripsDateTimes(final String stopId, final LocalDateTime currentDateTime) {
        Map<String, List<LocalDateTime>> tripsTimes = new HashMap<>();
        final int maxTimeSeconds = DISPLAY_TIME_MINUTES * 60;

        try {
            File stopTimesFile = new File(FILE_PATH_STOP_TIMES);
            Scanner stopTimesReader = new Scanner(stopTimesFile).useDelimiter(",");
            
            // Spusti opis podatkov
            stopTimesReader.nextLine();
   
            while (stopTimesReader.hasNextLine()) {
                String readTripId = stopTimesReader.next();
                // Preberi cas prihoda
                stopTimesReader.next();
                String readDepartureTime = stopTimesReader.next();

                // Stop ime najden dodaj
                if (stopId.compareTo(stopTimesReader.next()) == 0) {
                    
                    // Shrani trenuten cas
                    LocalTime tripTime = LocalTime.parse(readDepartureTime);
                    LocalDateTime tripDateTime = LocalDateTime.of(currentDateTime.toLocalDate(), tripTime);   
                   
                    // Izracunaj razliko v casu -> ce je negativna razlika je slo cez potem pristej dan
                    long timeDiff = currentDateTime.toLocalTime().until(tripDateTime, ChronoUnit.SECONDS);

                    if (timeDiff < 0) {
                        timeDiff += dayInSeconds;
                        tripDateTime = tripDateTime.plusDays(1);
                    } 

                    // Ce je razlika manjsa ali enaka od maksimuma ga dodaj
                    if (timeDiff <= maxTimeSeconds) {
                        tripsTimes.computeIfAbsent(readTripId, k -> new ArrayList<>()).add(tripDateTime);
                    }
                }
                stopTimesReader.nextLine();
            }
            stopTimesReader.close();
        } catch (FileNotFoundException e) {
            return null;
        }
        
        // Vrni
        return tripsTimes;
    }

    private static Map<String, String> getTripsRoutes(final Set<String> tripIds) {
        Map<String, String> tripsRoutes = new HashMap<>();
        try {
            File tripesFile = new File(FILE_PATH_TRIPS);
            Scanner tripsReader = new Scanner(tripesFile).useDelimiter(",");
            
            // Spusti opis podatkov
            tripsReader.nextLine();
   
            while (tripsReader.hasNextLine()) {
                String routeId = tripsReader.next();
                // Spusti service_id
                tripsReader.next();
                String tripId = tripsReader.next();

                tripsRoutes.putIfAbsent(tripId, routeId);
                
                tripsReader.nextLine();
            }
            tripsReader.close();
        } catch (FileNotFoundException e) {
            return null;
        }

        return tripsRoutes;
    }

    private static Map<String, String> getRoutesNames(final Set<String> routes) {
        Map<String, String> routesName = new HashMap<>();
        try {
            File routesFile = new File(FILE_PATH_ROUTES);
            Scanner routesReader = new Scanner(routesFile).useDelimiter(",");
            
            // Spusti opis podatkov
            routesReader.nextLine();
   
            while (routesReader.hasNextLine()) {
                String routeId = routesReader.next();
                // Spusti agency_id
                routesReader.next();
                String routeShortName = routesReader.next();

                // Ce je routeId na seznamu ga dodaj v routesName
                if (routes.contains(routeId))
                    routesName.putIfAbsent(routeId, routeShortName);
                
                routesReader.nextLine();
            }
            routesReader.close();
        } catch (FileNotFoundException e) {
            return null;
        }

        return routesName;
    }

    private static boolean printBusData(final String stopId, int numOfBuses, final DataType dataType) {
        LocalDateTime currentDateTime = LocalDateTime.now();

        // Pridobi case za vsak trip
        Map<String, List<LocalDateTime>> tripsTimes = getTripsDateTimes(stopId, currentDateTime);
        if (tripsTimes == null) return false;

        // Za vsak trip pridobi route
        Map<String, String> tripsRoutes = getTripsRoutes(tripsTimes.keySet());
        if (tripsRoutes == null) return false;

        // Pridobi imena poti
        Set<String> routesSet = new HashSet<>(tripsRoutes.values());
        Map<String, String> routesNames = getRoutesNames(routesSet);
        if (routesNames == null) return false;

        // Zdruzi poti z casi
        Map<String, List<LocalDateTime>> routesTimes = new HashMap<>();
        for (String tripId : tripsTimes.keySet()) {
            final String routeId = tripsRoutes.get(tripId);
            
            if (routesTimes.containsKey(routeId)) {
                routesTimes.get(routeId).addAll(tripsTimes.get(tripId));
            }
            else {
                routesTimes.put(routeId, new ArrayList<>(tripsTimes.get(tripId)));
            }
        }

        if (dataType == DataType.ABSOLUTE) numOfBuses = numOfBuses > ABSOLUTE_MAX_BUSES ? ABSOLUTE_MAX_BUSES : numOfBuses;
        else if (dataType == DataType.RELATIVE) numOfBuses = numOfBuses > RELATIVE_MAX_BUSES ? RELATIVE_MAX_BUSES : numOfBuses;


        // Posortiraj in izloci
        for (String routeId : routesTimes.keySet()) {
            Collections.sort(routesTimes.get(routeId));
            final int size = routesTimes.get(routeId).size();
   
            routesTimes.put(routeId, routesTimes.get(routeId).subList(0, 
                size < numOfBuses ? size : numOfBuses));
        }


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        final LocalTime currentTime = currentDateTime.toLocalTime();
        
        // Izpisi:
        for (String routeId : routesTimes.keySet()) {
            System.out.print(routesNames.get(routeId) + ": ");

            if (routesTimes.get(routeId).size() >= 1) {
                LocalTime ld = routesTimes.get(routeId).get(0).toLocalTime();
                if (dataType == DataType.ABSOLUTE) System.out.print(formatter.format(ld));
                else if (dataType == DataType.RELATIVE) {
                    long diff = diffTimeInSeconds(currentTime, ld) / 60;
                    System.out.print(diff + "min");
                }
            }
  

            for (int i = 1; i < routesTimes.get(routeId).size(); ++i) {
                LocalTime ld = routesTimes.get(routeId).get(i).toLocalTime();
                System.out.print(", ");
                if (dataType == DataType.ABSOLUTE) System.out.print(formatter.format(ld));
                else if (dataType == DataType.RELATIVE) {
                    long diff = diffTimeInSeconds(currentTime, ld) / 60;
                    System.out.print(diff + "min");
                }
            }
            System.out.println();
        }
        return true;
    }

    public static void main(String[] args) {
        // Preveri če je število podatkov pravilno
        if (args.length != 3){
            System.out.println("Napaka - Napacno stevilo podatkov");
            return;
        }

        // Preveri ce postaja obstaja v bazi
        if (stopExists(args[ARG_STOP_ID]) == false) {
            System.out.println("Napaka - Postaja ne obstaja");
            return;
        }

        String stopId = args[ARG_STOP_ID];

        // Preveri ce je vredu stevilo avtobusov
        if (isPositiveInteger(args[ARG_NUMBER_OF_BUSES]) == false) {
            System.out.println("Napaka - Ni legalno stevilo avtobusov");
            return;
        }

        int numBuses = Integer.parseInt(args[ARG_NUMBER_OF_BUSES]);

        // Določi tip izpisa podatkov
        DataType dataType;
        if (args[ARG_DATA_TYPE].compareTo(DATA_TYPE_ABSOLUTE) == 0) {
            dataType = DataType.ABSOLUTE;
        } else if (args[ARG_DATA_TYPE].compareTo(DATA_TYPE_RELATIVE) == 0) {
            dataType = DataType.RELATIVE;
        } else{
            System.out.println("Napaka - Napacen izpis podatkov");
            return;
        }

        String stopName = getStopName(stopId);
        if (stopName == null) {
            System.out.println("Napaka - Postaja ne obstaja");
            return;
        }
        System.out.println(stopName);

        if (printBusData(stopId, numBuses, dataType) == false) {
            System.out.println("Napaka - Postaja ne obstaja");
            return;
        }
    }
}
