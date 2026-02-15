# Prediction-of-VOIP-traffic-in-Real-mobile-networks
A Java based ML project to forecast VOIP traffic in real mobile networks
# VoIP Traffic Prediction using VAR Model

This project implements a complete **Vector Autoregression (VAR)** pipeline in Java for predicting VoIP traffic Quality of Service (QoS) metrics including MOS, Bandwidth, Jitter, RTT, DJB, and SNR.

The pipeline includes:

- Augmented Dickey-Fuller (ADF) stationarity testing
- Automatic differencing of non-stationary series
- Z-score normalization
- Optimal lag selection using AIC or BIC
- VAR model fitting using Ordinary Least Squares (OLS)
- Recursive multi-step forecasting
- Evaluation metrics (MAE, RMSE, MAPE)
- CSV export of predictions

---

## Predicted Metrics

- MOS – Mean Opinion Score  
- BW – Bandwidth  
- JIT – Jitter  
- RTT – Round Trip Time  
- DJB – Delay Jitter Buffer  
- SNR – Signal-to-Noise Ratio  

---

## Requirements

- Java 8+
- JAR dependencies in `lib/`:
  - `commons-math3-3.6.1.jar`
  - `opencsv-5.7.1.jar`
  - `commons-lang3-3.12.0.jar`

---

## Project Structure

```
VoIP-VAR-Prediction/
│
├── src/
│   └── VARPipelineFull.java
├── lib/
│   ├── commons-math3-3.6.1.jar
│   ├── opencsv-5.7.1.jar
│   └── commons-lang3-3.12.0.jar
└── README.md
```

---

## How to Compile

### Windows

```bash
javac -cp ".;lib/commons-math3-3.6.1.jar;lib/opencsv-5.7.1.jar;lib/commons-lang3-3.12.0.jar" src/VARPipelineFull.java
```

### Mac/Linux

```bash
javac -cp ".:lib/commons-math3-3.6.1.jar:lib/opencsv-5.7.1.jar:lib/commons-lang3-3.12.0.jar" src/VARPipelineFull.java
```

---

## How to Run

### Windows

```bash
java -cp ".;lib/commons-math3-3.6.1.jar;lib/opencsv-5.7.1.jar;lib/commons-lang3-3.12.0.jar;src" VARPipelineFull data/sample_data.csv
```

### Mac/Linux

```bash
java -cp ".:lib/commons-math3-3.6.1.jar:lib/opencsv-5.7.1.jar:lib/commons-lang3-3.12.0.jar:src" VARPipelineFull data/sample_data.csv
```

---

## Output

After execution:

- Console displays:
  - ADF test statistics
  - Selected optimal lag
  - Evaluation metrics (MAE, RMSE, MAPE)

- A file named:

```
VAR_predictions.csv
```

is generated containing:
- Timestamp
- Actual values
- Predicted values

---

## Configuration Parameters

Inside `VARPipelineFull.java`:

```java
static final double TRAIN_FRACTION = 0.7;
static final int P_MAX = 12;
static final boolean NORMALIZE = true;
static final boolean AUTO_ADF_DIFF = true;
static final boolean SELECT_BY_AIC = true;
```

These parameters allow easy experimentation and tuning.

---

## Academic Use

This project demonstrates:

- Multivariate Time Series Forecasting
- Econometric Modeling (VAR)
- Statistical Model Selection
- VoIP QoS Prediction
- Practical Implementation of ADF Testing

Suitable for research projects, final-year engineering projects, and academic submissions.
## Author

**Aravind J
