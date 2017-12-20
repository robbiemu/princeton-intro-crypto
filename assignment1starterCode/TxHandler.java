import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    private UTXOPool utxoPool;

    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        UTXOPool uniqueUtxos = new UTXOPool();

        double inSum = 0;
        double outSum = 0;

        List<Transaction.Input> ins = tx.getInputs();
        for(Transaction.Input i: ins) {
            UTXO utxo = new UTXO(i.prevTxHash, i.outputIndex);

            Transaction.Output o = utxoPool.getTxOutput(utxo); // this output is from the previous TXs. It is the input for this TX
            if (!utxoPool.contains(utxo)) // #1
                return false;

            if(!Crypto.verifySignature(o.address, tx.getRawDataToSign(ins.indexOf(i)), i.signature)) // #2
                return false;

            if(uniqueUtxos.contains(utxo)) // #3
                return false;

            uniqueUtxos.addUTXO(utxo, o);

            inSum += o.value;
        }

        for(Transaction.Output o: tx.getOutputs()) {
            if (o.value < 0) // #4
                return false;

            outSum += o.value;
        }

        return inSum >= outSum; // #5 -- shouldn't this be equals?
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Set<Transaction> acceptedTXs = new HashSet<>();

        for(Transaction t: possibleTxs) {
            if(!isValidTx(t))
                continue;

            for (Transaction.Input i : t.getInputs()) {
                UTXO utxo = new UTXO(i.prevTxHash, i.outputIndex);
                utxoPool.removeUTXO(utxo);
            }

            List<Transaction.Output> outs = t.getOutputs();
            for (Transaction.Output o : outs) {
                UTXO utxo = new UTXO(t.getHash(), outs.indexOf(o));
                utxoPool.addUTXO(utxo, o);
            }

            acceptedTXs.add(t);
        }

        return acceptedTXs.toArray(new Transaction [acceptedTXs.size()]);
    }

}
