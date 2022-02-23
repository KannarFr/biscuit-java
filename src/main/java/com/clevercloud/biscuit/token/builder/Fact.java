package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.SymbolTable;

import java.util.List;

public class Fact {
    Predicate predicate;

    public Fact(String name, List<Term> terms) {
        this.predicate = new Predicate(name, terms);
    }

    public Fact(Predicate p) {
        this.predicate = p;
    }

    public com.clevercloud.biscuit.datalog.Fact convert(SymbolTable symbols) {
        return new com.clevercloud.biscuit.datalog.Fact(this.predicate.convert(symbols));
    }

    public static Fact convert_from(com.clevercloud.biscuit.datalog.Fact f, SymbolTable symbols) {
        return new Fact(Predicate.convert_from(f.predicate(), symbols));
    }

    @Override
    public String toString() {
        return "fact("+predicate+")";
    }

    public String name() {
        return this.predicate.name;
    }

    public List<Term> terms() { return this.predicate.terms; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Fact fact = (Fact) o;

        return predicate != null ? predicate.equals(fact.predicate) : fact.predicate == null;
    }

    @Override
    public int hashCode() {
        return predicate != null ? predicate.hashCode() : 0;
    }
}
