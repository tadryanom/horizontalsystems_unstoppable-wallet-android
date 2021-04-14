package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.managers.RestoreSettingType
import io.horizontalsystems.coinkit.models.CoinType

fun CoinType.canSupport(accountType: AccountType) = when (this) {
    is CoinType.Bitcoin,
    is CoinType.Litecoin,
    is CoinType.BitcoinCash,
    is CoinType.Dash,
    is CoinType.Ethereum,
    is CoinType.Erc20 -> {
        accountType is AccountType.Mnemonic && accountType.words.size == 12 && accountType.salt == null
    }
    is CoinType.BinanceSmartChain,
    is CoinType.Bep20,
    is CoinType.Bep2 -> {
        accountType is AccountType.Mnemonic && accountType.words.size == 24 && accountType.salt == null
    }
    is CoinType.Zcash -> {
        accountType is AccountType.Zcash && accountType.words.size == 24
    }
    is CoinType.Unsupported -> false
}

val CoinType.title: String
    get() {
        return when (this) {
            is CoinType.Bitcoin -> "Bitcoin"
            is CoinType.Litecoin -> "Litecoin"
            is CoinType.BitcoinCash -> "BitcoinCash"
            else -> ""
        }
    }

val CoinType.label: String?
    get() = when (this) {
        is CoinType.Erc20 -> "ERC20"
        is CoinType.Bep20 -> "BEP20"
        is CoinType.Bep2 -> "BEP2"
        else -> null
    }

val CoinType.swappable: Boolean
    get() = this is CoinType.Ethereum || this is CoinType.Erc20 || this is CoinType.BinanceSmartChain || this is CoinType.Bep20

val CoinType.predefinedAccountType: PredefinedAccountType
    get() = when (this) {
        CoinType.Bitcoin, CoinType.Litecoin, CoinType.BitcoinCash, CoinType.Dash, CoinType.Ethereum, is CoinType.Erc20, is CoinType.Unsupported -> PredefinedAccountType.Standard
        CoinType.BinanceSmartChain, is CoinType.Bep20, is CoinType.Bep2 -> PredefinedAccountType.Binance
        CoinType.Zcash -> PredefinedAccountType.Zcash
    }

val CoinType.coinSettingTypes: List<CoinSettingType>
    get() = when (this) {
        CoinType.Bitcoin,
        CoinType.Litecoin -> listOf(CoinSettingType.derivation)
        CoinType.BitcoinCash -> listOf(CoinSettingType.bitcoinCashCoinType)
        else -> listOf()
    }

val CoinType.defaultSettingsArray: List<CoinSettings>
    get() = when (this) {
        CoinType.Bitcoin,
        CoinType.Litecoin -> listOf(CoinSettings(mapOf(CoinSettingType.derivation to AccountType.Derivation.bip49.value)))
        CoinType.BitcoinCash -> listOf(CoinSettings(mapOf(CoinSettingType.bitcoinCashCoinType to BitcoinCashCoinType.type145.value)))
        else -> listOf()
    }

val CoinType.restoreSettingTypes: List<RestoreSettingType>
    get() = when (this) {
        CoinType.Zcash -> listOf(RestoreSettingType.birthdayHeight)
        else -> listOf()
    }
