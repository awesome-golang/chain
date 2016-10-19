import java.util.*;

import com.chain.api.*;
import com.chain.http.*;
import com.chain.signing.*;

class Assets {
  public static void main(String[] args) throws Exception {
    Context context = new Context();

    MockHsm.Key assetKey = MockHsm.Key.create(context);
    HsmSigner.addKey(assetKey, MockHsm.getSignerContext(context));

    MockHsm.Key accountKey = MockHsm.Key.create(context);
    HsmSigner.addKey(accountKey, MockHsm.getSignerContext(context));

    new Account.Builder()
      .setAlias("acme_treasury")
      .addRootXpub(accountKey.xpub)
      .setQuorum(1)
      .create(context);

    // snippet create-asset-acme-common
    new Asset.Builder()
      .setAlias("acme_common")
      .addRootXpub(assetKey.xpub)
      .setQuorum(1)
      .addTag("internal_rating", "1")
      .addDefinitionField("issuer", "Acme Inc.")
      .addDefinitionField("type", "security")
      .addDefinitionField("subtype", "private")
      .addDefinitionField("class", "common")
      .create(context);
    // endsnippet

    // snippet create-asset-acme-preferred
    new Asset.Builder()
      .setAlias("acme_preferred")
      .addRootXpub(assetKey.xpub)
      .setQuorum(1)
      .addTag("internal_rating", "2")
      .addDefinitionField("issuer", "Acme Inc.")
      .addDefinitionField("type", "security")
      .addDefinitionField("subtype", "private")
      .addDefinitionField("class", "perferred")
      .create(context);
    // endsnippet

    // snippet list-local-assets
    Asset.Items localAssets = new Asset.QueryBuilder()
      .setFilter("is_local=$1")
      .addFilterParameter("yes")
      .execute(context);

    while (localAssets.hasNext()) {
      Asset asset = localAssets.next();
      System.out.println("Local asset: " + asset.alias);
    }
    // endsnippet

    // snippet list-private-preferred-securities
    Asset.Items common = new Asset.QueryBuilder()
      .setFilter("definition.type=$1 AND definition.subtype=$2 AND definition.class=$3")
      .addFilterParameter("security")
      .addFilterParameter("private")
      .addFilterParameter("preferred")
      .execute(context);
    // endsnippet

    // snippet build-issue
    Transaction.Template issuanceTransaction = new Transaction.Builder()
      .addAction(new Transaction.Action.Issue()
        .setAssetAlias("acme_common")
        .setAmount(1000)
      ).addAction(new Transaction.Action.ControlWithAccount()
        .setAccountAlias("acme_treasury")
        .setAssetAlias("acme_common")
        .setAmount(1000)
      ).build(context);
    // endsnippet

    // snippet sign-issue
    Transaction.Template signedIssuanceTransaction = HsmSigner.sign(issuanceTransaction);
    // endsnippet

    // snippet submit-issue
    Transaction.submit(context, signedIssuanceTransaction);
    // endsnippet

    ControlProgram externalProgram = new ControlProgram.Builder()
      .controlWithAccountByAlias("acme_treasury")
      .create(context);

    // snippet external-issue
    Transaction.Template externalIssuance = new Transaction.Builder()
      .addAction(new Transaction.Action.Issue()
        .setAssetAlias("acme_preferred")
        .setAmount(2000)
      ).addAction(new Transaction.Action.ControlWithProgram()
        .setControlProgram(externalProgram)
        .setAssetAlias("acme_preferred")
        .setAmount(2000)
      ).build(context);

    Transaction.submit(context, HsmSigner.sign(externalIssuance));
    // endsnippet

    // snippet build-retire
    Transaction.Template retirementTransaction = new Transaction.Builder()
      .addAction(new Transaction.Action.SpendFromAccount()
        .setAccountAlias("acme_treasury")
        .setAssetAlias("acme_common")
        .setAmount(50)
      ).addAction(new Transaction.Action.Retire()
        .setAssetAlias("acme_common")
        .setAmount(50)
      ).build(context);
    // endsnippet

    // snippet sign-retire
    Transaction.Template signedRetirementTransaction = HsmSigner.sign(retirementTransaction);
    // endsnippet

    // snippet submit-retire
    Transaction.submit(context, signedRetirementTransaction);
    // endsnippet

    // snippet list-issuances
    Transaction.Items acmeCommonIssuances = new Transaction.QueryBuilder()
      .setFilter("inputs(action=$1 AND asset_alias=$2)")
      .addFilterParameter("issue")
      .addFilterParameter("acme_common")
      .execute(context);

    while (acmeCommonIssuances.hasNext()) {
      Transaction tx = acmeCommonIssuances.next();
      System.out.println("Acme Common issued in tx " + tx.id);
    }
    // endsnippet

    // snippet list-transfers
    Transaction.Items acmeCommonTransfers = new Transaction.QueryBuilder()
      .setFilter("inputs(action=$1 AND asset_alias=$2)")
      .addFilterParameter("spend")
      .addFilterParameter("acme_common")
      .execute(context);

    while (acmeCommonTransfers.hasNext()) {
      Transaction tx = acmeCommonTransfers.next();
      System.out.println("Acme Common transferred in tx " + tx.id);
    }
    // endsnippet

    // snippet list-retirements
    Transaction.Items acmeCommonRetirements = new Transaction.QueryBuilder()
      .setFilter("outputs(action=$1 AND asset_alias=$2)")
      .addFilterParameter("retire")
      .addFilterParameter("acme_common")
      .execute(context);

    while (acmeCommonRetirements.hasNext()) {
      Transaction tx = acmeCommonRetirements.next();
      System.out.println("Acme Common retired in tx " + tx.id);
    }
    // endsnippet

    // snippet list-acme-common-balance
    Balance.Items acmeCommonBalances = new Balance.QueryBuilder()
      .setFilter("asset_alias=$1")
      .addFilterParameter("acme_common")
      .execute(context);

    Balance acmeCommonBalance = acmeCommonBalances.next();
    System.out.println("Total circulation of Acme Common: " + acmeCommonBalance.amount);
    // endsnippet

    // snippet list-acme-balance
    Balance.Items acmeAnyBalances = new Balance.QueryBuilder()
      .setFilter("asset_definition.issuer=$1")
      .addFilterParameter("Acme Inc.")
      .execute(context);

    while (acmeAnyBalances.hasNext()) {
      Balance stockBalance = acmeAnyBalances.next();
      System.out.println(
        "Total circulation of Acme stock " + stockBalance.sumBy.get("asset_alias") +
        ": " + stockBalance.amount
      );
    }
    // endsnippet

    // snippet list-acme-common-unspents
    UnspentOutput.Items acmeCommonUnspentOutputs = new UnspentOutput.QueryBuilder()
      .setFilter("asset_alias=$1")
      .addFilterParameter("acme_common")
      .execute(context);

    while (acmeCommonUnspentOutputs.hasNext()) {
      UnspentOutput utxo = acmeCommonUnspentOutputs.next();
      System.out.println("Acme Common held in output " + utxo.transactionId + ":" + utxo.position);
    }
    // endsnippet
  }
}