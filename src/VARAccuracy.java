import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.*;
import java.util.*;

public class VARAccuracy {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java VARAccuracy <csv_file>");
            return;
        }

        String csvFile = args[0];
        List<String> times = new ArrayList<>();
        double[][] data = loadCSV(csvFile, times);

        int trainSize = 700;
        int nVars = 6;

        double[][] train = Arrays.copyOfRange(data, 0, trainSize);
        double[][] test = Arrays.copyOfRange(data, trainSize, data.length);

        double[][] A1 = new double[nVars][nVars];
        double[] c = new double[nVars];
        estimateVAR1(train, A1, c);

        double[][] predictions = new double[test.length][nVars];
        double[] last = train[trainSize - 1];

        for (int t = 0; t < test.length; t++) {
            double[] pred = new double[nVars];
            for (int i = 0; i < nVars; i++) {
                pred[i] = c[i];
                for (int j = 0; j < nVars; j++) {
                    pred[i] += A1[i][j] * last[j];
                }
            }
            predictions[t] = pred;
            last = pred;
        }

        computeMetrics(test, predictions);
        saveResults(times, trainSize, test, predictions, "VAR_predictions.csv");
    }

    public static double[][] loadCSV(String filename, List<String> times) throws Exception {
        List<double[]> values = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(filename))) {
            String[] line;
            boolean firstLine = true;
            while ((line = reader.readNext()) != null) {
                if (firstLine) { firstLine = false; continue; }
                times.add(line[0]);
                double[] row = new double[6];
                for (int i = 0; i < 6; i++) {
                    row[i] = Double.parseDouble(line[i+1].trim());
                }
                values.add(row);
            }
        }
        return values.toArray(new double[0][0]);
    }

    public static void estimateVAR1(double[][] train, double[][] A1, double[] c) {
        int T = train.length;
        int k = train[0].length;
        double[][] X = new double[T-1][k+1];
        double[][] Y = new double[T-1][k];

        for (int t = 1; t < T; t++) {
            X[t-1][0] = 1.0;
            for (int j = 0; j < k; j++) X[t-1][j+1] = train[t-1][j];
            for (int i = 0; i < k; i++) Y[t-1][i] = train[t][i];
        }

        for (int i = 0; i < k; i++) {
            double[] coef = ols(X, Y, i);
            c[i] = coef[0];
            for (int j = 0; j < k; j++) A1[i][j] = coef[j+1];
        }
    }

    public static double[] ols(double[][] X, double[][] Y, int col) {
        int T = X.length;
        int k = X[0].length;
        double[][] XtX = new double[k][k];
        double[] XtY = new double[k];

        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                double sum = 0;
                for (int t = 0; t < T; t++) sum += X[t][i] * X[t][j];
                XtX[i][j] = sum;
            }
            double sum2 = 0;
            for (int t = 0; t < T; t++) sum2 += X[t][i] * Y[t][col];
            XtY[i] = sum2;
        }
        return gaussianElimination(XtX, XtY);
    }

    public static double[] gaussianElimination(double[][] A, double[] b) {
        int n = b.length;
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            int max = i;
            for (int j = i+1; j < n; j++) if (Math.abs(A[j][i]) > Math.abs(A[max][i])) max = j;
            double[] temp = A[i]; A[i] = A[max]; A[max] = temp;
            double t = b[i]; b[i] = b[max]; b[max] = t;

            for (int j = i+1; j < n; j++) {
                double factor = A[j][i]/A[i][i];
                for (int k = i; k < n; k++) A[j][k] -= factor * A[i][k];
                b[j] -= factor * b[i];
            }
        }

        for (int i = n-1; i >=0; i--) {
            double sum = b[i];
            for (int j = i+1; j < n; j++) sum -= A[i][j]*x[j];
            x[i] = sum / A[i][i];
        }
        return x;
    }

    public static void computeMetrics(double[][] actual, double[][] pred) {
        int nVars = actual[0].length;
        int nRows = actual.length;

        for (int i = 0; i < nVars; i++) {
            double sumAbs = 0, sumSq = 0;
            for (int t = 0; t < nRows; t++) {
                double err = actual[t][i] - pred[t][i];
                sumAbs += Math.abs(err);
                sumSq += err*err;
            }
            double mae = sumAbs / nRows;
            double rmse = Math.sqrt(sumSq / nRows);
            System.out.printf("Variable %d -> MAE: %.4f, RMSE: %.4f%n", i+1, mae, rmse);
        }
    }

    public static void saveResults(List<String> times, int trainSize, double[][] actual, double[][] pred, String outFile) throws Exception {
        try (CSVWriter writer = new CSVWriter(new FileWriter(outFile))) {
            String[] header = {"Time","MOS_actual","MOS_pred","BW_actual","BW_pred","JIT_actual","JIT_pred","RTT_actual","RTT_pred","DJB_actual","DJB_pred","SNR_actual","SNR_pred"};
            writer.writeNext(header);

            for (int t = 0; t < actual.length; t++) {
                String[] row = new String[13];
                row[0] = times.get(trainSize + t);
                for (int i = 0; i < 6; i++) {
                    row[1 + i*2] = String.valueOf(actual[t][i]);
                    row[2 + i*2] = String.valueOf(pred[t][i]);
                }
                writer.writeNext(row);
            }
        }
        System.out.println("Predictions saved to " + outFile);
    }
}
