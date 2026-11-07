package org.mr0btain.tunerstoolkit.components.parsers.a2l.variantcoding
/*
/*
VARIANT_CODING
*/
variant_coding_exp : VARIANT_CODING;
variant_coding_block : BEGIN variant_coding_exp variant_coding_sub_nodes END VARIANT_CODING;
variant_coding_sub_nodes : (
                             var_characteristic_block
                           | var_criterion_block
                           | var_forbidden_comb_block
                           | var_naming_exp
                           | var_separator_exp
                           )*;

var_characteristic_exp : VAR_CHARACTERISTIC Name=IDENTIFIER CriterionNames+=IDENTIFIER*;
var_characteristic_block : BEGIN var_characteristic_exp var_characteristic_sub_nodes END VAR_CHARACTERISTIC;
var_characteristic_sub_nodes : (
                                 var_address_block
                                )*;

var_criterion_exp : VAR_CRITERION Name=IDENTIFIER LongIdentifier=string_exp Values+=IDENTIFIER*;
var_criterion_block : BEGIN var_criterion_exp var_criterion_sub_nodes END VAR_CRITERION;
var_criterion_sub_nodes : (
                            var_measurement_exp
                          | var_selection_characteristic_exp
                          )*;

var_measurement_exp : VAR_MEASUREMENT Name=IDENTIFIER;
var_selection_characteristic_exp : VAR_SELECTION_CHARACTERISTIC Name=IDENTIFIER;

var_forbidden_comb_block : BEGIN VAR_FORBIDDEN_COMB tuples+=criterion_tuple* END VAR_FORBIDDEN_COMB;
criterion_tuple : CriterionName=IDENTIFIER CriterionValue=IDENTIFIER;

var_naming_exp : VAR_NAMING Tag=(NUMERIC | ALPHA);
var_separator_exp : VAR_SEPARATOR Separator=string_exp;

 */
class VariantCodingParser {
    // TODO
}