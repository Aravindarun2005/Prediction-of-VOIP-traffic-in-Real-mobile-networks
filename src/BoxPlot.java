import com.opencsv.CSVReader;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import javax.swing.*;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class BoxPlot {
    public static void main(String[] args) {
        String csvFile = "first.csv"; // <-- replace with your dataset file
        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] headers = reader.readNext(); // first row = headers

            // prepare lists for six QoS variables
            List<Double> mos = new ArrayList<>();
            List<Double> delay = new ArrayList<>();
            List<Double> jitter = new ArrayList<>();
            List<Double> throughput = new ArrayList<>();
            List<Double> packetLoss = new ArrayList<>();
            List<Double> bandwidth = new ArrayList<>();

            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length >= 6) {
                    mos.add(Double.parseDouble(line[0]));
                    delay.add(Double.parseDouble(line[1]));
                    jitter.add(Double.parseDouble(line[2]));
                    throughput.add(Double.parseDouble(line[3]));
                    packetLoss.add(Double.parseDouble(line[4]));
                    bandwidth.add(Double.parseDouble(line[5]));
                }
            }

            // add data into dataset for boxplot
            dataset.add(mos, "MOS", "MOS");
            dataset.add(delay, "Delay", "Delay");
            dataset.add(jitter, "Jitter", "Jitter");
            dataset.add(throughput, "Throughput", "Throughput");
            dataset.add(packetLoss, "Packet Loss", "Packet Loss");
            dataset.add(bandwidth, "Bandwidth", "Bandwidth");

        } catch (Exception e) {
            e.printStackTrace();
        }

        // create boxplot chart
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
                "QoS Metrics Boxplot", // chart title
                "QoS Metric",          // x-axis label
                "Value",               // y-axis label
                dataset,
                true
        );

        // customize plot
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();

        renderer.setFillBox(true);
        renderer.setSeriesPaint(0, new java.awt.Color(200, 200, 200)); // light gray box
        renderer.setSeriesOutlinePaint(0, java.awt.Color.BLACK);       // black outline
        renderer.setArtifactPaint(java.awt.Color.BLACK);               // median line in black
        renderer.setUseOutlinePaintForWhiskers(true);
        renderer.setWhiskerWidth(0.8);
        renderer.setMeanVisible(false);                                // hide mean dots
        renderer.setDefaultOutlinePaint(java.awt.Color.RED);           // red for outliers
        plot.setRenderer(renderer);

        // set Y-axis range 0–250 with tick unit 50
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setRange(0, 250);
        yAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(50));

        // show chart
        JFrame frame = new JFrame("Boxplot Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);
    }
}
