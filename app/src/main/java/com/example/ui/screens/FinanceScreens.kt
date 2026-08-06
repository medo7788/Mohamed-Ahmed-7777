package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LivePricesRepository
import com.example.ui.theme.CustomThemeColors
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.Spacing
import com.example.model.CalcKey
import androidx.compose.foundation.BorderStroke
import kotlin.math.max
import kotlin.math.pow

@Composable
fun FinanceInputField(label: String, value: String, colors: CustomThemeColors, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border,
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
            focusedLabelColor = colors.accent
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
    )
}

// DiscountCalcScreen is now implemented in DiscountCalcScreen.kt with commercial grade features

// LoanCalcScreen is now implemented in LoanCalcScreen.kt with commercial grade features

// SavingsCalcScreen is now implemented in SavingsCalcScreen.kt with commercial grade features

// SalesTaxCalcScreen is now implemented in SalesTaxCalcScreen.kt with commercial grade features

// TipCalcScreen is now implemented in TipCalcScreen.kt with commercial grade features

// PercentageCalcScreen is now implemented in PercentageCalcScreen.kt with commercial grade features

// UnitPriceCalcScreen is now implemented in UnitPriceCalcScreen.kt with commercial grade features
