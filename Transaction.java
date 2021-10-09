import java.security.*;
import java.util.ArrayList;

public class Transaction {

    public String transactionId; // Contains a hash of transaction*
    public PublicKey sender; // Senders address/public key.
    public PublicKey recipient; // Recipients address/public key.
    public float value; // Contains the amount we wish to send to the recipient.
    public byte[] signature; // This is to prevent anybody else from spending funds in our wallet.

    public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

    private static int sequence = 0; // A rough count of how many transactions have been generated

    // Constructor:
    public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
    }

    public boolean processTransaction() {

        if (!verifySignature()) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }

        // Gathers transaction inputs (Making sure they are unspent):
        for (TransactionInput i : inputs) {
            i.UTXO = TCoin.UTXOs.get(i.transactionOutputId);
        }

        // Checks if transaction is valid:
        if (getInputsValue() < TCoin.minTransact) {
            System.out.println("Transaction Inputs too small: " + getInputsValue());
            System.out.println("Please enter the amount greater than " + TCoin.minTransact);
            return false;
        }

        // Generate transaction outputs:
        float leftOver = getInputsValue() - value; // get value of inputs then the left over change:
        transactionId = calculateHash();
        outputs.add(new TransactionOutput(this.recipient, value, transactionId)); // send value to recipient
        outputs.add(new TransactionOutput(this.sender, leftOver, transactionId)); // send the left over 'change' back to
                                                                                  // sender

        // Add outputs to Unspent list
        for (TransactionOutput o : outputs) {
            TCoin.UTXOs.put(o.id, o);
        }

        // Remove transaction inputs from UTXO lists as spent:
        for (TransactionInput i : inputs) {
            if (i.UTXO == null)
                continue; // Transaction skips if not found.
            TCoin.UTXOs.remove(i.UTXO.id);
        }

        return true;
    }

    public float getInputsValue() {
        float total = 0;
        for (TransactionInput i : inputs) {
            if (i.UTXO == null)
                continue; // Transaction skips if not found.
            total += i.UTXO.value;
        }
        return total;
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient) + value;
        signature = StringUtil.applyECDSASig(privateKey, data);
    }

    public boolean verifySignature() {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient) + value;
        return StringUtil.verifyECDSASig(sender, data, signature);
    }

    public float getOutputsValue() {
        float total = 0;
        for (TransactionOutput o : outputs) {
            total += o.value;
        }
        return total;
    }

    private String calculateHash() {
        sequence++; // increase the sequence to avoid 2 identical transactions having the same hash
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient) + value + sequence);
    }
}