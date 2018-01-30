package com.shopify.ui.payment.card

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import com.domain.entity.Card
import com.shopify.ShopifyWrapper
import com.shopify.api.R
import com.shopify.constant.CARD_PAYMENT
import com.shopify.constant.Extra
import com.shopify.ui.payment.card.contract.CardPresenter
import com.shopify.ui.payment.card.contract.CardView
import com.shopify.ui.payment.card.di.CardPaymentModule
import com.ui.base.lce.BaseActivity
import com.ui.base.lce.view.LceLayout
import com.ui.base.picker.BaseBottomSheetPicker
import com.ui.custom.SimpleTextWatcher
import com.ui.ext.hideKeyboard
import kotlinx.android.synthetic.main.activity_card.*
import javax.inject.Inject

class CardActivity : BaseActivity<Pair<Card, String>, CardView, CardPresenter>(), CardView {

    companion object {

        fun getStartIntent(context: Context): Intent {
            val intent = Intent(context, CardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_FORWARD_RESULT
            return intent
        }
    }

    @Inject
    lateinit var cardPresenter: CardPresenter
    private lateinit var fieldTextWatcher: TextWatcher
    private lateinit var monthPicker: DateBottomSheetPicker
    private lateinit var yearPicker: DateBottomSheetPicker

    //ANDROID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(getString(R.string.credit_card))
        setupPickers()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        if (this::fieldTextWatcher.isInitialized) {
            holderNameInput.addTextChangedListener(fieldTextWatcher)
            cardNumberInput.addTextChangedListener(fieldTextWatcher)
            monthInput.addTextChangedListener(fieldTextWatcher)
            yearInput.addTextChangedListener(fieldTextWatcher)
            cvvInput.addTextChangedListener(fieldTextWatcher)
        }
    }

    override fun onPause() {
        super.onPause()
        holderNameInput.removeTextChangedListener(fieldTextWatcher)
        cardNumberInput.removeTextChangedListener(fieldTextWatcher)
        monthInput.removeTextChangedListener(fieldTextWatcher)
        yearInput.removeTextChangedListener(fieldTextWatcher)
        cvvInput.removeTextChangedListener(fieldTextWatcher)
    }

    //INIT

    override fun getContentView() = R.layout.activity_card

    override fun createPresenter() = cardPresenter

    override fun inject() {
        ShopifyWrapper.component.attachCardPaymentComponent(CardPaymentModule()).inject(this)
    }

    //SETUP

    private fun setupPickers() {
        monthPicker = DateBottomSheetPicker.newInstance(DateBottomSheetPicker.DATE_TYPE_MONTH)
        monthPicker.onDoneButtonClickedListener = object : BaseBottomSheetPicker.OnDoneButtonClickedListener {
            override fun onDoneButtonClicked(selectedData: String) {
                monthInput.setText(selectedData)
            }
        }
        yearPicker = DateBottomSheetPicker.newInstance(DateBottomSheetPicker.DATE_TYPE_YEAR)
        yearPicker.onDoneButtonClickedListener = object : BaseBottomSheetPicker.OnDoneButtonClickedListener {
            override fun onDoneButtonClicked(selectedData: String) {
                yearInput.setText(selectedData)
            }
        }
    }

    private fun setupListeners() {
        fieldTextWatcher = object : SimpleTextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                checkInputFields()
            }
        }
        monthInput.setOnClickListener {
            it.hideKeyboard()
            showMonthPicker()
        }
        yearInput.setOnClickListener {
            it.hideKeyboard()
            yearPicker.show(supportFragmentManager, DateBottomSheetPicker.DATE_TYPE_YEAR)
        }
        submitButton.setOnClickListener { loadData() }
        cardNumberInput.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                v.hideKeyboard()
                v.clearFocus()
                showMonthPicker()
                true
            } else {
                false
            }
        }
    }

    private fun showMonthPicker() {
        monthPicker.show(supportFragmentManager, DateBottomSheetPicker.DATE_TYPE_MONTH)
    }

    private fun checkInputFields() {
        submitButton.isEnabled = holderNameInput.text.isNotBlank() &&
                cardNumberInput.text.isNotBlank() &&
                monthInput.text.isNotBlank() &&
                yearInput.text.isNotBlank() &&
                cvvInput.text.isNotBlank()
    }

    //LCE

    override fun loadData(pullToRefresh: Boolean) {
        super.loadData(pullToRefresh)
        presenter.processCardData(
            holderNameInput.text.toString(),
            cardNumberInput.text.toString(),
            monthInput.text.toString(),
            yearInput.text.toString(),
            cvvInput.text.toString()
        )
    }

    override fun showContent(data: Pair<Card, String>) {
        super.showContent(data)
        val result = Intent()
        result.putExtra(Extra.CARD, data.first)
        result.putExtra(Extra.CARD_TOKEN, data.second)
        result.putExtra(Extra.PAYMENT_TYPE, CARD_PAYMENT)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    override fun cardValidationError(error: Int) {
        changeState(LceLayout.LceState.ContentState)
        showMessage(error)
    }
}