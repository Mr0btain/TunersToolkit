package org.mr0btain.tunerstoolkit.components.parsers.a2l
class FutureProof {
    /*

    I cannot find any definition for the following elements which are part of 1.7.1



    TYPEDEF_CHARACTERISTIC
    <guessed definition from demo file for 1.7.1>

    typedef_characteristic_exp : TYPEDEF_CHARACTERISTIC IDENTIFIER string_exp type_enum IDENTIFIER value IDENTIFIER value value;
    typedef_characteristic_block : BEGIN typedef_characteristic_exp typedef_characteristic_sub_nodes END TYPEDEF_CHARACTERISTIC;
    typedef_characteristic_sub_nodes : (
    extended_limits_exp
    | format_exp
    | phys_unit_exp
    | axis_descr_block
    | number_exp
    )*;


    INSTANCE
    <guessed definition from demo file for 1.7.1>

    instance_exp : INSTANCE IDENTIFIER string_exp IDENTIFIER int_value;
    instance_block : BEGIN instance_exp instance_sub_nodes END INSTANCE;
    instance_sub_nodes : (
    matrix_dim_exp
    | display_identifier_exp
    )*;


    TYPEDEF_AXIS
    <guessed definition from demo file for 1.7.1>

    typedef_axis_exp : TYPEDEF_AXIS IDENTIFIER string_exp IDENTIFIER IDENTIFIER value IDENTIFIER int_value value value;
    typedef_axis_block : BEGIN typedef_axis_exp END TYPEDEF_AXIS;
    // there is no example within the demo file for optional parameters
    typedef_axis_sub_nodes : ;


    TYPEDEF_STRUCTURE
    <guessed definition from demo file for 1.7.1>

    typedef_structure_exp : TYPEDEF_STRUCTURE IDENTIFIER string_exp int_value;
    typedef_structure_block : BEGIN typedef_structure_exp typedef_structure_sub_nodes END TYPEDEF_STRUCTURE;
    typedef_structure_sub_nodes : (
    structure_component_block
    )*;

    structure_component_exp : STRUCTURE_COMPONENT IDENTIFIER IDENTIFIER int_value;
    structure_component_block : BEGIN structure_component_exp structure_component_sub_nodes END STRUCTURE_COMPONENT;
    // there is no example within the demo file for optional parameters
    structure_component_sub_nodes : (
    matrix_dim_exp
    )*;


    TRANSFORMER
    <guessed definition from demo file for 1.7.1>

    transformer_exp : TRANSFORMER IDENTIFIER string_exp string_exp string_exp int_value trigger_conditions_enum IDENTIFIER;
    transformer_block : BEGIN transformer_exp transformer_sub_nodes END TRANSFORMER;
    transformer_sub_nodes : (
    transformer_in_objects_block
    | transformer_out_objects_block
    )*;

    trigger_conditions_enum : ON_CHANGE | ON_USER_REQUEST;

    transformer_in_objects_block : BEGIN TRANSFORMER_IN_OBJECTS Identifier+=IDENTIFIER* END TRANSFORMER_IN_OBJECTS;
    transformer_out_objects_block : BEGIN TRANSFORMER_OUT_OBJECTS Identifier+=IDENTIFIER* END TRANSFORMER_OUT_OBJECTS;
     */
}