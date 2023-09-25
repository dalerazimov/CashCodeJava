package tj.epic.cashcode;

/**
 * Event callbacks for bill validator state changes during polling.
 */
public interface CashCodeEvents {
    void onAccept();
    void onReject();
    void onEscrowPosition();
    void onStack();
    void onBillStack(int value);
    void onReturn();
    void onBillReturned();
    void onDropCassetteOutOfPosition();
    void onCassetteInitialize();
}
