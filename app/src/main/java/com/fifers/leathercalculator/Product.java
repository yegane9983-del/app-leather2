package com.fifers.leathercalculator;

/** A catalog product with its standard (expected) leather usage per unit, in feet ("پا"). */
final class Product {
    final String name;
    final double defaultUsage;

    Product(String name, double defaultUsage) {
        this.name = name;
        this.defaultUsage = defaultUsage;
    }
}
