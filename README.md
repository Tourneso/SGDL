# SGDL: Smart Contract Vulnerability Generation via Deep Learning

## Task Definition

Generation reentrancy, timestamp dependence, integer overflow and underflow, Use tx.origin for authentication, Unhandled Exception, Unchecked send and Short Address Attack vulnerabilities in smart contract.

## Structure in this project

```
${SGDL}
├── Classification
│   ├── svm.py
├── Generation
    ├── LeakGAN.py
├── Injection
    ├── deleteStatement.java
    ├── Extract.java
    ├── ExtractFunc.java
    ├── ExtractOverFlow.java
    ├── ExtractTx.java
    ├── ExtractUnhandledException.java
    ├── FuncToVecMain.java
    ├── Inject.java
    ├── InvalidSafeMath.java
    ├── Main.java
```

- `Classification`: This is the model for classification data.
- `Generation`: This is the training and testing model of generation data.
- `Injection`: This is to extract code slicing information of vulnerability and injection.
