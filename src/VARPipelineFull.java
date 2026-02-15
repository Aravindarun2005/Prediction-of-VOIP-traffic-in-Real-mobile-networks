import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.math3.linear.*;

import java.io.*;
import java.util.*;

public class VARPipelineFull {

    static final int N_VARS = 6;
    static final double TRAIN_FRACTION = 0.7;
    static final int P_MAX = 12;
    static final boolean NORMALIZE = true;
    static final boolean AUTO_ADF_DIFF = true;
    static final boolean SELECT_BY_AIC = true;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java -cp \".;commons-math3-3.6.1.jar;opencsv-5.7.1.jar;commons-lang3-3.12.0.jar\" VARPipelineFull <csv_file>");
            return;
        }

        String csvFile = args[0];
        List<String> times = new ArrayList<>();
        double[][] data = DataIO.loadCSV(csvFile, times);

        System.out.println("Loaded rows: " + data.length);

        if (AUTO_ADF_DIFF) {
            System.out.println("Running ADF test per series and differencing non-stationary series...");
            boolean anyDiff;
            do {
                anyDiff = false;
                for (int j = 0; j < N_VARS; j++) {
                    double[] col = getColumn(data, j);
                    ADFTest.Result r = ADFTest.runADF(col, 1);
                    System.out.printf("Series %d: ADF t-stat = %.5f, approx p ~ %.3f -> %s%n",
                            j + 1, r.tStatistic, r.pValue, (r.pValue < 0.05 ? "Stationary" : "Non-stationary"));
                    if (r.pValue >= 0.05) {
                        data = DataIO.differenceColumn(data, j);
                        if (!times.isEmpty()) times.remove(0);
                        anyDiff = true;
                        System.out.println(" -> Differenced column " + (j + 1) + " and restarted ADF checks (length now " + data.length + ").");
                        break;
                    }
                }
            } while (anyDiff);
        }

        double[][] origMeansStd = null;
        if (NORMALIZE) {
            origMeansStd = DataIO.zScoreNormalizeInPlace(data);
            System.out.println("Normalized data (z-score).");
        }

        int trainSize = (int)Math.round(data.length * TRAIN_FRACTION);
        if (trainSize <= P_MAX) trainSize = Math.max(P_MAX + 1, data.length - 1);
        double[][] train = Arrays.copyOfRange(data, 0, trainSize);
        double[][] test = Arrays.copyOfRange(data, trainSize, data.length);
        System.out.println("Train size: " + train.length + ", Test size: " + test.length);

        System.out.println("Selecting lag p in 1.." + Math.min(P_MAX, train.length - 1) + " using " + (SELECT_BY_AIC ? "AIC" : "BIC"));
        int bestP = 1;
        double bestIC = Double.POSITIVE_INFINITY;
        VARModel bestModel = null;
        for (int p = 1; p <= Math.min(P_MAX, train.length - 1); p++) {
            VARModel model = new VARModel(N_VARS, p);
            model.fit(train);
            Residuals resid = model.computeResiduals(train);
            double aic = IC.computeAIC(resid, train.length, N_VARS, p);
            double bic = IC.computeBIC(resid, train.length, N_VARS, p);
            System.out.printf("p=%2d   AIC=%.6f   BIC=%.6f%n", p, aic, bic);
            double ic = SELECT_BY_AIC ? aic : bic;
            if (ic < bestIC) {
                bestIC = ic;
                bestP = p;
                bestModel = model;
            }
        }
        System.out.println("Selected p* = " + bestP + " (IC=" + bestIC + ")");

        bestModel.fit(train);

        double[][] preds = bestModel.forecastRecursive(train, test.length);

        if (NORMALIZE && origMeansStd != null) {
            DataIO.unNormalizeInPlace(test, origMeansStd);
            DataIO.unNormalizePredictions(preds, origMeansStd);
            System.out.println("Converted predictions and test back to original scale for metric reporting.");
        }

        System.out.println("\nEvaluation (on processed scale or original if un-normalized):");
        Metrics.computeMetricsBySeries(test, preds);

        ResultsSaver.saveResults(times, trainSize, test, preds, "VAR_predictions.csv");
        System.out.println("Saved VAR_predictions.csv");
    }

    static double[] getColumn(double[][] data, int col) {
        double[] out = new double[data.length];
        for (int i = 0; i < data.length; i++) out[i] = data[i][col];
        return out;
    }

    static class DataIO {
        public static double[][] loadCSV(String filename, List<String> times) throws Exception {
            List<double[]> rows = new ArrayList<>();
            try (CSVReader r = new CSVReader(new FileReader(filename))) {
                String[] line;
                boolean first = true;
                while ((line = r.readNext()) != null) {
                    if (first) { first = false; continue; }
                    if (line.length < N_VARS + 1) continue;
                    times.add(line[0]);
                    double[] row = new double[N_VARS];
                    for (int j = 0; j < N_VARS; j++) row[j] = Double.parseDouble(line[j+1].trim());
                    rows.add(row);
                }
            }
            return rows.toArray(new double[0][0]);
        }

        public static double[][] differenceColumn(double[][] data, int col) {
            int T = data.length;
            double[][] res = new double[T - 1][N_VARS];
            for (int t = 1; t < T; t++) {
                for (int j = 0; j < N_VARS; j++) {
                    if (j == col) res[t-1][j] = data[t][j] - data[t-1][j];
                    else res[t-1][j] = data[t][j];
                }
            }
            return res;
        }

        public static double[][] zScoreNormalizeInPlace(double[][] data) {
            int T = data.length;
            double[][] ms = new double[N_VARS][2];
            for (int j = 0; j < N_VARS; j++) {
                double mean = 0;
                for (int t = 0; t < T; t++) mean += data[t][j];
                mean /= T;
                double var = 0;
                for (int t = 0; t < T; t++) var += Math.pow(data[t][j] - mean, 2);
                var /= T;
                double std = Math.sqrt(var);
                if (std == 0) std = 1.0;
                for (int t = 0; t < T; t++) data[t][j] = (data[t][j] - mean) / std;
                ms[j][0] = mean;
                ms[j][1] = std;
            }
            return ms;
        }

        public static void unNormalizeInPlace(double[][] data, double[][] ms) {
            int T = data.length;
            for (int j = 0; j < N_VARS; j++) {
                double mean = ms[j][0], std = ms[j][1];
                for (int t = 0; t < T; t++) data[t][j] = data[t][j] * std + mean;
            }
        }

        public static void unNormalizePredictions(double[][] preds, double[][] ms) {
            for (int j = 0; j < N_VARS; j++) {
                double mean = ms[j][0], std = ms[j][1];
                for (int t = 0; t < preds.length; t++) preds[t][j] = preds[t][j] * std + mean;
            }
        }
    }

    static class ADFTest {
        static class Result { double tStatistic; double pValue; Result(double t, double p){ this.tStatistic = t; this.pValue = p; } }

        public static Result runADF(double[] y, int lag) {
            int T = y.length;
            if (T < 10) return new Result(0.0, 1.0);
            double[] dy = new double[T-1];
            for (int i = 1; i < T; i++) dy[i-1] = y[i] - y[i-1];

            int rows = T - 1 - lag;
            if (rows <= 3) return new Result(0.0, 1.0);

            double[][] X = new double[rows][3 + lag];
            double[] Y = new double[rows];
            for (int t = lag; t < T - 1; t++) {
                int r = t - lag;
                Y[r] = dy[t];
                X[r][0] = 1.0;
                X[r][1] = t + 1;
                X[r][2] = y[t];
                for (int l = 1; l <= lag; l++) X[r][2 + l] = dy[t - l];
            }

            RealMatrix Xmat = MatrixUtils.createRealMatrix(X);
            RealVector Yvec = new ArrayRealVector(Y);
            RealMatrix XtX = Xmat.transpose().multiply(Xmat);
            DecompositionSolver solver = new SingularValueDecomposition(XtX).getSolver();
            RealVector coef = solver.solve(Xmat.transpose().operate(Yvec));
            RealVector resid = Yvec.subtract(Xmat.operate(coef));

            int df = Y.length - X[0].length;
            double s2 = resid.dotProduct(resid) / df;
            RealMatrix cov = new LUDecomposition(XtX).getSolver().getInverse();
            double stderrGamma = Math.sqrt(Math.abs(cov.getEntry(2,2) * s2));
            double tstat = coef.getEntry(2) / stderrGamma;

            double pApprox = approximatePvalueADF(tstat);
            return new Result(tstat, pApprox);
        }

        private static double approximatePvalueADF(double tstat) {
            double t = tstat;
            if (t <= -3.43) return 0.01;
            if (t <= -2.86) return 0.05;
            if (t <= -2.57) return 0.10;
            return 0.20;
        }
    }

    static class VARModel {
        int n; int p;
        RealMatrix B;

        VARModel(int nVars, int lag) { this.n = nVars; this.p = lag; }

        public void fit(double[][] data) {
            int T = data.length;
            int rows = T - p;
            double[][] X = new double[rows][1 + n * p];
            double[][] Y = new double[rows][n];

            for (int t = p; t < T; t++) {
                int r = t - p;
                X[r][0] = 1.0;
                for (int lag = 1; lag <= p; lag++) {
                    double[] past = data[t - lag];
                    for (int j = 0; j < n; j++) X[r][(lag-1)*n + 1 + j] = past[j];
                }
                System.arraycopy(data[t], 0, Y[r], 0, n);
            }

            RealMatrix Xmat = MatrixUtils.createRealMatrix(X);
            RealMatrix Ymat = MatrixUtils.createRealMatrix(Y);
            RealMatrix XtX = Xmat.transpose().multiply(Xmat);

            DecompositionSolver solver;
            try {
                solver = new LUDecomposition(XtX).getSolver();
                if (!solver.isNonSingular()) throw new Exception("singular");
            } catch (Exception ex) {
                solver = new SingularValueDecomposition(XtX).getSolver();
            }
            this.B = solver.solve(Xmat.transpose().multiply(Ymat));
        }

        public Residuals computeResiduals(double[][] data) {
            int T = data.length;
            int rows = T - p;
            double[][] e = new double[rows][n];
            for (int t = p; t < T; t++) {
                double[] x = new double[1 + n*p];
                x[0] = 1.0;
                for (int lag = 1; lag <= p; lag++)
                    for (int j = 0; j < n; j++)
                        x[(lag-1)*n + 1 + j] = data[t - lag][j];
                double[] yhat = B.transpose().operate(x);
                for (int j = 0; j < n; j++) e[t-p][j] = data[t][j] - yhat[j];
            }
            return new Residuals(e);
        }

        public double[][] forecastRecursive(double[][] train, int steps) {
            if (train.length < p) throw new IllegalArgumentException("train length < p");
            Deque<double[]> window = new ArrayDeque<>();
            for (int i = train.length - p; i < train.length; i++) window.addLast(Arrays.copyOf(train[i], n));
            double[][] preds = new double[steps][n];
            for (int t = 0; t < steps; t++) {
                double[] x = new double[1 + n*p];
                x[0] = 1.0;
                double[][] arr = window.toArray(new double[0][0]);
                for (int lag = 1; lag <= p; lag++)
                    for (int j = 0; j < n; j++)
                        x[(lag-1)*n + 1 + j] = arr[arr.length - lag][j];
                double[] yhat = B.transpose().operate(x);
                preds[t] = yhat;
                window.pollFirst();
                window.addLast(Arrays.copyOf(yhat, n));
            }
            return preds;
        }
    }

    static class Residuals { double[][] e; Residuals(double[][] e){ this.e = e; } }

    static class IC {
        public static double computeAIC(Residuals resid, int T, int N, int p) {
            int L = resid.e.length;
            RealMatrix cov = covMatrix(resid.e);
            double logdet = safeLogDet(cov);
            double penalty = (2.0 * p * N * N) / (double)L;
            return logdet + penalty;
        }
        public static double computeBIC(Residuals resid, int T, int N, int p) {
            int L = resid.e.length;
            RealMatrix cov = covMatrix(resid.e);
            double logdet = safeLogDet(cov);
            double penalty = (Math.log(L) * p * N * N) / (double)L;
            return logdet + penalty;
        }
        private static RealMatrix covMatrix(double[][] e) {
            int L = e.length, N = e[0].length;
            RealMatrix M = MatrixUtils.createRealMatrix(N, N);
            double[] mean = new double[N];
            for (int t = 0; t < L; t++) for (int i = 0; i < N; i++) mean[i] += e[t][i];
            for (int i = 0; i < N; i++) mean[i] /= L;
            for (int t = 0; t < L; t++) for (int i = 0; i < N; i++) for (int j = 0; j < N; j++)
                M.addToEntry(i, j, (e[t][i] - mean[i]) * (e[t][j] - mean[j]));
            return M.scalarMultiply(1.0 / L);
        }
        private static double safeLogDet(RealMatrix M) {
            double det;
            try { det = new LUDecomposition(M).getDeterminant(); } catch (Exception ex) { det = 1e-12; }
            if (det <= 0 || Double.isNaN(det) || Double.isInfinite(det)) det = 1e-12;
            return Math.log(det);
        }
    }

    static class Metrics {
        public static void computeMetricsBySeries(double[][] actual, double[][] pred) {
            int T = actual.length, N = actual[0].length;
            for (int j = 0; j < N; j++) {
                double sumAbs = 0, sumSq = 0, sumPct = 0;
                int validPct = 0;
                for (int t = 0; t < T; t++) {
                    double a = actual[t][j], p = pred[t][j];
                    double err = a - p;
                    sumAbs += Math.abs(err);
                    sumSq += err*err;
                    if (a != 0) { sumPct += Math.abs(err / a); validPct++; }
                }
                double mae = sumAbs / T;
                double rmse = Math.sqrt(sumSq / T);
                double mape = validPct > 0 ? (sumPct / validPct) * 100.0 : Double.NaN;
                System.out.printf("Series %d -> MAE: %.6f, RMSE: %.6f, MAPE: %s%n", j+1, mae, rmse, (Double.isNaN(mape) ? "NaN" : String.format("%.3f%%", mape)));
            }
        }
    }

    static class ResultsSaver {
        public static void saveResults(List<String> times, int trainSize, double[][] actual, double[][] pred, String file) throws Exception {
            try (CSVWriter w = new CSVWriter(new FileWriter(file))) {
                String[] header = new String[1 + N_VARS*2];
                header[0] = "Time";
                String[] names = {"MOS","BW","JIT","RTT","DJB","SNR"};
                for (int i = 0; i < N_VARS; i++) { header[1 + i*2] = names[i] + "_actual"; header[1 + i*2 + 1] = names[i] + "_pred"; }
                w.writeNext(header);
                for (int t = 0; t < actual.length; t++) {
                    String[] row = new String[1 + N_VARS*2];
                    int timeIdx = trainSize + t;
                    row[0] = (times.size() > timeIdx ? times.get(timeIdx) : Integer.toString(timeIdx));
                    for (int j = 0; j < N_VARS; j++) {
                        row[1 + j*2] = Double.toString(actual[t][j]);
                        row[1 + j*2 + 1] = Double.toString(pred[t][j]);
                    }
                    w.writeNext(row);
                }
            }
        }
    }
}
