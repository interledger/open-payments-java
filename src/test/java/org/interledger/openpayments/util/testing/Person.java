package org.interledger.openpayments.util.testing;

/**
 * Immutable test record representing a person, primarily used for serialization/deserialization
 * testing.
 *
 * @param name the person's name
 * @param age the person's age in years
 */
public record Person(String name, int age) {}
