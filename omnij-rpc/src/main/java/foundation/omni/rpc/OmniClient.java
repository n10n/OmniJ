package foundation.omni.rpc;

import com.fasterxml.jackson.databind.JavaType;
import com.msgilligan.bitcoinj.rpc.BitcoinExtendedClient;
import com.msgilligan.bitcoinj.rpc.JsonRPCException;
import com.msgilligan.bitcoinj.rpc.RPCConfig;
import foundation.omni.CurrencyID;
import foundation.omni.Ecosystem;
import foundation.omni.OmniValue;
import foundation.omni.PropertyType;
import foundation.omni.json.conversion.OmniClientModule;
import foundation.omni.net.OmniNetworkParameters;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

// TODO: add missing RPCs:
// - listtransactions_MP
// - omni_gettradehistoryforpair
// - omni_gettradehistoryforaddress

// TODO: replace deprecated RPCs for 0.0.10:
// - send_MP -> omni_send
// - sendtoowners_MP -> omni_sendsto
// - sendrawtx_MP -> omni_sendrawtx
// - getinfo_MP -> omni_getinfo
// - getbalance_MP -> omni_getbalance
// - getallbalancesforid_MP -> omni_getallbalancesforid
// - getallbalancesforaddress_MP -> omni_getallbalancesforaddress
// - gettransaction_MP -> omni_gettransaction
// - listtransactions_MP -> omni_listtransactions
// - listblocktransactions_MP -> omni_listblocktransactions
// - getactivedexsells_MP -> omni_getactivedexsells
// - listproperties_MP -> omni_listproperties
// - getproperty_MP -> omni_getproperty
// - getactivecrowdsales_MP -> omni_getactivecrowdsales
// - getcrowdsale_MP -> omni_getcrowdsale
// - getgrants_MP -> omni_getgrants
// - getsto_MP -> omni_getsto

/**
 * Pure Java Bitcoin and Omni Core JSON-RPC client with camelCase method names.
 * <p>
 * For example, if the RPC is {@code "omni_getbalance"}, then the corresponding method name is {@link #omniGetBalance(Address, CurrencyID)}.
 *
 * @see <a href="https://github.com/OmniLayer/omnicore/blob/omnicore-0.0.10/src/omnicore/doc/rpc-api.md">Omni Core RPC API documentation on GitHub</a>
 */
public class OmniClient extends BitcoinExtendedClient {

    public static Sha256Hash zeroHash = Sha256Hash.wrap("0000000000000000000000000000000000000000000000000000000000000000");
    private DecimalFormat jsonDecimalFormat;

    public OmniClient(RPCConfig config) throws IOException {
        this(config.getNetParams(), config.getURI(), config.getUsername(), config.getPassword());
    }

    public OmniClient(NetworkParameters netParams, URI server, String rpcuser, String rpcpassword) throws IOException {
        super(netParams, server, rpcuser, rpcpassword);
        mapper.registerModule(new OmniClientModule(getNetParams()));
        // Create a DecimalFormat that fits our requirements
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        String pattern = "#,##0.0#";
        jsonDecimalFormat = new DecimalFormat(pattern, symbols);
        jsonDecimalFormat.setParseBigDecimal(true);
    }

    public OmniNetworkParameters getOmniNetParams() {
        return OmniNetworkParameters.fromBitcoinParms(getNetParams());
    }

    /**
     * Returns various state information of Omni Core and the Omni Layer protocol.
     *
     * @return An object with state information
     */
    public Map<String, Object> omniGetInfo() throws JsonRPCException, IOException {
        Map<String, Object> result = send("getinfo_MP");
        return result;
    }

    /**
     * Lists all currencies, smart properties and tokens.
     *
     * @return A list with short information
     */
    public List<SmartPropertyListInfo> omniListProperties() throws JsonRPCException, IOException {
        JavaType resultType = mapper.getTypeFactory().constructCollectionType(List.class, SmartPropertyListInfo.class);
        return send("listproperties_MP", resultType);
    }

    /**
     * Returns information about the given currency, property, or token.
     *
     * @param currency The identifier to look up
     * @return An object with detailed information
     */
    public Map<String, Object> omniGetProperty(CurrencyID currency) throws JsonRPCException, IOException {
        Map<String, Object> result = send("getproperty_MP", currency);
        return result;
    }

    /**
     * Returns information about a crowdsale.
     *
     * @param currency The identifier of the crowdsale
     * @return An object with detailed information
     */
    public Map<String, Object> omniGetCrowdsale(CurrencyID currency) throws JsonRPCException, IOException {
        Map<String, Object> result = send("getcrowdsale_MP", currency);
        return result;
    }

    /**
     * Lists currently active crowdsales.
     *
     * @return A list with information about active crowdsales
     */
    public List<Map<String, Object>> omniGetActiveCrowdsales() throws JsonRPCException, IOException {
        List<Map<String, Object>> result = send("getactivecrowdsales_MP");
        return result;
    }

    /**
     * Lists currently active offers on the distributed BTC/OMNI exchange.
     *
     * @return A list with information about the active offers
     */
    public List<Map<String, Object>> omniGetActiveDExSells() throws JsonRPCException, IOException {
        List<Map<String, Object>> result = send("getactivedexsells_MP");
        return result;
    }

    /**
     * Returns the balance for a given address and property.
     *
     * @param address  The address to look up
     * @param currency The identifier of the token to look up
     * @return The available and reserved balance
     */
    public BalanceEntry omniGetBalance(Address address, CurrencyID currency)
            throws JsonRPCException, IOException, ParseException {
        return send("getbalance_MP", BalanceEntry.class, address, currency.getValue());
    }

    /**
     * Returns a list of balances for a given identifier.
     *
     * @param currency The identifier of the token to look up
     * @return A Sorted Map indexed by addresses to available and reserved balances
     */
    public SortedMap<Address, BalanceEntry> omniGetAllBalancesForId(CurrencyID currency)
            throws JsonRPCException, IOException, ParseException, AddressFormatException {
        return send("getallbalancesforid_MP", AddressBalanceEntries.class, currency);
    }

    /**
     * Returns a list of all token balances for a given address.
     *
     * @param address The address to look up
     * @return A Sorted Map indexed by currency/propertyid to available and reserved balances
     */
    public SortedMap<CurrencyID, BalanceEntry> omniGetAllBalancesForAddress(Address address)
            throws JsonRPCException, IOException, ParseException {
        return send("getallbalancesforaddress_MP", PropertyBalanceEntries.class, address);
    }

    /**
     * Returns information about an Omni Layer transaction.
     *
     * @param txid The hash of the transaction to look up
     * @return Information about the transaction
     */
    public Map<String, Object> omniGetTransaction(Sha256Hash txid) throws JsonRPCException, IOException {
        Map<String, Object> transaction = send("gettransaction_MP", txid);
        return transaction;
    }

    /**
     * Lists all Omni transactions in a block.
     *
     * @param blockIndex The block height or block index
     * @return A list of transaction hashes
     */
    public List<Sha256Hash> omniListBlockTransactions(Integer blockIndex) throws JsonRPCException, IOException {
        JavaType resultType = mapper.getTypeFactory().constructCollectionType(List.class, Sha256Hash.class);
        return send("listblocktransactions_MP", resultType, blockIndex);
    }

    /**
     * Broadcasts a raw Omni Layer transaction.
     *
     * @param fromAddress The address to send from
     * @param rawTxHex    The hex-encoded raw transaction
     * @return The hash of the transaction
     */
    public Sha256Hash omniSendRawTx(Address fromAddress, String rawTxHex) throws JsonRPCException, IOException {
        return omniSendRawTx(fromAddress, rawTxHex, null);
    }

    /**
     * Broadcasts a raw Omni Layer transaction with reference address.
     *
     * @param fromAddress      The address to send from
     * @param rawTxHex         The hex-encoded raw transaction
     * @param referenceAddress The reference address
     * @return The hash of the transaction
     */
    public Sha256Hash omniSendRawTx(Address fromAddress, String rawTxHex, Address referenceAddress)
            throws JsonRPCException, IOException {
        return send("sendrawtx_MP", Sha256Hash.class, fromAddress, rawTxHex, referenceAddress);
    }

    /**
     * Creates and broadcasts a "simple send" transaction.
     *
     * @param fromAddress The address to spent from
     * @param toAddress   The address to send to
     * @param currency    The identifier of the token to transfer
     * @param amount      The amount to transfer
     * @return The hash of the transaction
     */
    public Sha256Hash omniSend(Address fromAddress, Address toAddress, CurrencyID currency, OmniValue amount)
            throws JsonRPCException, IOException {
        return send("send_MP", Sha256Hash.class, fromAddress, toAddress, currency, amount);
    }

    /**
     * Creates and broadcasts a "send to owners" transaction.
     *
     * @param fromAddress The address to spent from
     * @param currency    The identifier of the token to distribute
     * @param amount      The amount to distribute
     * @return The hash of the transaction
     */
    public Sha256Hash omniSendSTO(Address fromAddress, CurrencyID currency, OmniValue amount)
            throws JsonRPCException, IOException {
        return send("sendtoowners_MP", Sha256Hash.class, fromAddress, currency, amount);
    }

    /**
     * Creates and broadcasts a "send all" transaction.
     *
     * @param fromAddress The address to spent from
     * @param toAddress   The address to send to
     * @param ecosystem   The ecosystem of the tokens to send
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendAll(Address fromAddress, Address toAddress, Ecosystem ecosystem)
            throws JsonRPCException, IOException {
        return send("omni_sendall", Sha256Hash.class, fromAddress, toAddress, ecosystem);
    }

    /**
     * Creates an offer on the traditional distributed exchange.
     *
     * @param fromAddress   The address
     * @param currencyId    The identifier of the currency for sale
     * @param amountForSale The amount of currency (BigDecimal coins)
     * @param amountDesired The amount of desired Bitcoin (in BTC)
     * @param paymentWindow The payment window measured in blocks
     * @param commitmentFee The minimum transaction fee required to be paid as commitment when accepting this offer
     * @param action        The action applied to the offer (1 = new, 2 = update, 3 = cancel)
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendDExSell(Address fromAddress, CurrencyID currencyId, OmniValue amountForSale,
                                      Coin amountDesired, Byte paymentWindow, Coin commitmentFee,
                                      Byte action)
            throws JsonRPCException, IOException {
        return send("omni_senddexsell", Sha256Hash.class, fromAddress, currencyId, amountForSale, amountDesired,
                                                            paymentWindow, commitmentFee, action);
    }

    /**
     * Create and broadcast an accept order for the specified token and amount.
     *
     * @param fromAddress The address to send from
     * @param toAddress   The address of the seller
     * @param currencyId  The identifier of the token to purchase
     * @param amount      The amount to accept
     * @param override    Override minimum accept fee and payment window checks (use with caution!)
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendDExAccept(Address fromAddress, Address toAddress, CurrencyID currencyId,
                                        OmniValue amount, Boolean override)
            throws JsonRPCException, IOException {
        return send("omni_senddexaccept", Sha256Hash.class, fromAddress, toAddress, currencyId, amount, override);
    }

    /**
     * Place a trade offer on the distributed token exchange.
     *
     * @param fromAddress     The address to trade with
     * @param propertyForSale The identifier of the tokens to list for sale
     * @param amountForSale   The amount of tokens to list for sale
     * @param propertyDesired The identifier of the tokens desired in exchange
     * @param amountDesired   The amount of tokens desired in exchange
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendTrade(Address fromAddress, CurrencyID propertyForSale, OmniValue amountForSale,
                                    CurrencyID propertyDesired, OmniValue amountDesired)
            throws JsonRPCException, IOException {
        return send("omni_sendtrade", Sha256Hash.class, fromAddress, propertyForSale, amountForSale,
                                                                        propertyDesired, amountDesired);
    }

    /**
     * Cancel offers on the distributed token exchange with the specified price.
     *
     * @param fromAddress     The address to trade with
     * @param propertyForSale The identifier of the tokens to list for sale
     * @param amountForSale   The amount of tokens to list for sale
     * @param propertyDesired The identifier of the tokens desired in exchange
     * @param amountDesired   The amount of tokens desired in exchange
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendCancelTradesByPrice(Address fromAddress, CurrencyID propertyForSale,
                                                  OmniValue amountForSale, CurrencyID propertyDesired,
                                                  OmniValue amountDesired)
            throws JsonRPCException, IOException {
        return send("omni_sendcanceltradesbyprice", Sha256Hash.class, fromAddress, propertyForSale, amountForSale,
                                                                                    propertyDesired, amountDesired);
    }

    /**
     * Cancel all offers on the distributed token exchange with the given currency pair.
     *
     * @param fromAddress     The address to trade with
     * @param propertyForSale The identifier of the tokens listed for sale
     * @param propertyDesired The identifier of the tokens desired in exchange
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendCancelTradesByPair(Address fromAddress, CurrencyID propertyForSale,
                                                 CurrencyID propertyDesired)
            throws JsonRPCException, IOException {
        return send("omni_sendcanceltradesbypair", Sha256Hash.class, fromAddress, propertyForSale, propertyDesired);
    }

    /**
     * Cancel all offers on the distributed token exchange with the given currency pair.
     *
     * @param fromAddress The address to trade with
     * @param ecosystem   The ecosystem of the offers to cancel: (1) main, (2) test
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendCancelAllTrades(Address fromAddress, Ecosystem ecosystem)
            throws JsonRPCException, IOException {
        return send("omni_sendcancelalltrades", Sha256Hash.class, fromAddress, ecosystem);
    }

    /**
     * Create new tokens with fixed supply.
     *
     * @param fromAddress  The address to send from
     * @param ecosystem    The ecosystem to create the tokens in
     * @param propertyType The type of the tokens to create
     * @param previousId   An identifier of a predecessor token (0 for new tokens)
     * @param category     A category for the new tokens (can be "")
     * @param subCategory  A subcategory for the new tokens (can be "")
     * @param name         The name of the new tokens to create
     * @param url          An URL for further information about the new tokens (can be "")
     * @param data         A description for the new tokens (can be "")
     * @param amount       The number of tokens to create
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendIssuanceFixed(Address fromAddress, Ecosystem ecosystem, PropertyType propertyType,
                                            CurrencyID previousId, String category, String subCategory, String name,
                                            String url, String data, OmniValue amount)
            throws JsonRPCException, IOException {
        return send("omni_sendissuancefixed", Sha256Hash.class, fromAddress, ecosystem,  propertyType, previousId,
                                                        category, subCategory, name, url, data, amount);
    }

    /**
     * Create new tokens as crowdsale.
     *
     * @param fromAddress     The address to send from
     * @param ecosystem       The ecosystem to create the tokens in
     * @param propertyType    The type of the tokens to create
     * @param previousId      An identifier of a predecessor token (0 for new tokens)
     * @param category        A category for the new tokens (can be "")
     * @param subCategory     A subcategory for the new tokens (can be "")
     * @param name            The name of the new tokens to create
     * @param url             An URL for further information about the new tokens (can be "")
     * @param data            A description for the new tokens (can be "")
     * @param propertyDesired the identifier of a token eligible to participate in the crowdsale
     * @param tokensPerUnit   the amount of tokens granted per unit invested in the crowdsale
     * @param deadline        the deadline of the crowdsale as Unix timestamp
     * @param earlyBirdBonus  an early bird bonus for participants in percent per week
     * @param issuerBonus     a percentage of tokens that will be granted to the issuer
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendIssuanceCrowdsale(Address fromAddress, Ecosystem ecosystem, PropertyType propertyType,
                                                CurrencyID previousId, String category, String subCategory, String name,
                                                String url, String data, CurrencyID propertyDesired,
                                                BigDecimal tokensPerUnit, Long deadline, Byte earlyBirdBonus,
                                                Byte issuerBonus)
            throws JsonRPCException, IOException {
        return send("omni_sendissuancecrowdsale", Sha256Hash.class, fromAddress, ecosystem, propertyType, previousId,
                category, subCategory, name, url, data,
                propertyDesired, tokensPerUnit.toPlainString(), deadline, earlyBirdBonus, issuerBonus);
    }

    /**
     * Manually close a crowdsale.
     *
     * @param fromAddress The address associated with the crowdsale to close
     * @param propertyId  The identifier of the crowdsale to close
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendCloseCrowdsale(Address fromAddress, CurrencyID propertyId)
            throws JsonRPCException, IOException {
        return send("omni_sendclosecrowdsale", Sha256Hash.class, fromAddress, propertyId);
    }

    /**
     * Create new tokens with manageable supply.
     *
     * @param fromAddress  The address to send from
     * @param ecosystem    The ecosystem to create the tokens in
     * @param propertyType The type of the tokens to create
     * @param previousId   An identifier of a predecessor token (0 for new tokens)
     * @param category     A category for the new tokens (can be "")
     * @param subCategory  A subcategory for the new tokens (can be "")
     * @param name         The name of the new tokens to create
     * @param url          An URL for further information about the new tokens (can be "")
     * @param data         A description for the new tokens (can be "")
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendIssuanceManaged(Address fromAddress, Ecosystem ecosystem, PropertyType propertyType,
                                              CurrencyID previousId, String category, String subCategory, String name,
                                              String url, String data)
            throws JsonRPCException, IOException {
        return send("omni_sendissuancemanaged", Sha256Hash.class, fromAddress, ecosystem, propertyType, previousId,
                                                        category, subCategory, name, url, data);
    }

    /**
     * Issue or grant new units of managed tokens.
     *
     * @param fromAddress The address to send from
     * @param toAddress   The receiver of the tokens
     * @param propertyId  The identifier of the tokens to grant
     * @param amount      The amount of tokens to create
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendGrant(Address fromAddress, Address toAddress, CurrencyID propertyId, OmniValue amount)
            throws JsonRPCException, IOException {
        return send("omni_sendgrant", Sha256Hash.class, fromAddress, toAddress, propertyId, amount);
    }

    /**
     * Revoke units of managed tokens.
     *
     * @param fromAddress The address to revoke the tokens from
     * @param propertyId  The identifier of the tokens to revoke
     * @param amount      The amount of tokens to revoke
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendRevoke(Address fromAddress, CurrencyID propertyId, OmniValue amount)
            throws JsonRPCException, IOException {
        return send("omni_sendrevoke", Sha256Hash.class, fromAddress, propertyId, amount);
    }

    /**
     * Change the issuer on record of the given tokens.
     *
     * @param fromAddress The address associated with the tokens
     * @param toAddress   The address to transfer administrative control to
     * @param propertyId  The identifier of the tokens
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendChangeIssuer(Address fromAddress, Address toAddress, CurrencyID propertyId)
            throws JsonRPCException, IOException {
        return send("omni_sendchangeissuer", Sha256Hash.class, fromAddress, toAddress, propertyId);
    }

    /**
     * Activates a protocol feature.
     *
     * @param fromAddress  The address to send from
     * @param featureId    The identifier of the feature to activate
     * @param block        The activation block
     * @param minVersion   The minimum supported client version
     * @return The hash of the transaction
     * @since Omni Core 0.0.10
     */
    public Sha256Hash omniSendActivation(Address fromAddress, Short featureId, Integer block, Integer minVersion)
            throws JsonRPCException, IOException {
        return send("omni_sendactivation", Sha256Hash.class, fromAddress, featureId, block, minVersion);
    }

    /**
     * Get information and recipients of a send-to-owners transaction.
     *
     * @param txid  The hash of the transaction to lookup
     * @return Information about the transaction
     */
    public Map<String, Object> omniGetSTO(Sha256Hash txid) throws JsonRPCException, IOException {
        String filter = "*"; // no filter at all
        Map<String, Object> stoInfo = send("getsto_MP", txid, filter);
        return stoInfo;
    }

    /**
     * Returns information about an order on the distributed token exchange.
     *
     * @param txid The transaction hash of the order to look up
     * @return Information about the order, trade, and order matches
     * @since Omni Core 0.0.10
     */
    public Map<String, Object> omniGetTrade(Sha256Hash txid) throws JsonRPCException, IOException {
        Map<String, Object> trade = send("omni_gettrade", txid);
        return trade;
    }

    /**
     * Lists orders on the distributed token exchange with the given token for sale.
     *
     * @param propertyForSale The identifier of the token for sale, used as filter
     * @return A list of orders
     * @since Omni Core 0.0.10
     */
    public List<Map<String, Object>> omniGetOrderbook(CurrencyID propertyForSale) throws JsonRPCException, IOException {
        List<Map<String, Object>> orders = send("omni_getorderbook", propertyForSale);
        return orders;
    }

    /**
     * Lists orders on the distributed token exchange with the given token for sale, and token desired.
     *
     * @param propertyForSale The identifier of the token for sale, used as filter
     * @param propertyDesired The identifier of the token desired, used as filter
     * @return A list of orders
     * @since Omni Core 0.0.10
     */
    public List<Map<String, Object>> omniGetOrderbook(CurrencyID propertyForSale, CurrencyID propertyDesired)
            throws JsonRPCException, IOException {
        List<Map<String, Object>> orders = send("omni_getorderbook", propertyForSale, propertyDesired);
        return orders;
    }

    /**
     * Returns information about granted and revoked units of managed tokens.
     *
     * @param propertyid The identifier of the managed tokens to lookup
     * @return A list of grants and revokes
     */
    public List<Map<String, Object>> omniGetGrants(CurrencyID propertyid) throws JsonRPCException, IOException {
        List<Map<String, Object>> orders = send("getgrants_MP", propertyid);
        return orders;
    }

    /**
     * Returns pending and completed feature activations.
     *
     * @return Pending and complete feature activations
     * @since Omni Core 0.0.10
     */
    public Map<String, List<Map<String, Object>>> omniGetActivations() throws JsonRPCException, IOException {
        Map<String, List<Map<String, Object>>> activations = send("omni_getactivations");
        return activations;
    }
}
