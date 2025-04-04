package org.biscuitsec.biscuit.token.builder;

import static org.biscuitsec.biscuit.token.UnverifiedBiscuit.defaultSymbolTable;

import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.SchemaVersion;
import org.biscuitsec.biscuit.datalog.SymbolTable;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.Block;
import org.biscuitsec.biscuit.token.builder.parser.Parser;

public final class Biscuit {
  private SecureRandom rng;
  private org.biscuitsec.biscuit.crypto.Signer root;
  private String context;
  private List<Fact> facts;
  private List<Rule> rules;
  private List<Check> checks;
  private List<Scope> scopes;
  private Option<Integer> rootKeyId;

  public Biscuit(final SecureRandom rng, final org.biscuitsec.biscuit.crypto.Signer root) {
    this.rng = rng;
    this.root = root;
    this.context = "";
    this.facts = new ArrayList<>();
    this.rules = new ArrayList<>();
    this.checks = new ArrayList<>();
    this.scopes = new ArrayList<>();
    this.rootKeyId = Option.none();
  }

  public Biscuit(
      final SecureRandom rng,
      final org.biscuitsec.biscuit.crypto.Signer root,
      Option<Integer> rootKeyId) {
    this.rng = rng;
    this.root = root;
    this.context = "";
    this.facts = new ArrayList<>();
    this.rules = new ArrayList<>();
    this.checks = new ArrayList<>();
    this.scopes = new ArrayList<>();
    this.rootKeyId = rootKeyId;
  }

  public Biscuit(
      final SecureRandom rng,
      final org.biscuitsec.biscuit.crypto.Signer root,
      Option<Integer> rootKeyId,
      org.biscuitsec.biscuit.token.builder.Block block) {
    this.rng = rng;
    this.root = root;
    this.rootKeyId = rootKeyId;
    this.context = block.context();
    this.facts = block.facts();
    this.rules = block.rules();
    this.checks = block.checks();
    this.scopes = block.scopes();
  }

  public Biscuit addAuthorityFact(org.biscuitsec.biscuit.token.builder.Fact f)
      throws Error.Language {
    f.validate();
    this.facts.add(f);
    return this;
  }

  public Biscuit addAuthorityFact(String s) throws Error.Parser, Error.Language {
    Either<
            org.biscuitsec.biscuit.token.builder.parser.Error,
            Tuple2<String, org.biscuitsec.biscuit.token.builder.Fact>>
        res = Parser.fact(s);

    if (res.isLeft()) {
      throw new Error.Parser(res.getLeft());
    }

    Tuple2<String, org.biscuitsec.biscuit.token.builder.Fact> t = res.get();

    return addAuthorityFact(t._2);
  }

  public Biscuit addAuthorityRule(org.biscuitsec.biscuit.token.builder.Rule rule) {
    this.rules.add(rule);
    return this;
  }

  public Biscuit addAuthorityRule(String s) throws Error.Parser {
    Either<
            org.biscuitsec.biscuit.token.builder.parser.Error,
            Tuple2<String, org.biscuitsec.biscuit.token.builder.Rule>>
        res = Parser.rule(s);

    if (res.isLeft()) {
      throw new Error.Parser(res.getLeft());
    }

    Tuple2<String, org.biscuitsec.biscuit.token.builder.Rule> t = res.get();

    return addAuthorityRule(t._2);
  }

  public Biscuit addAuthorityCheck(org.biscuitsec.biscuit.token.builder.Check c) {
    this.checks.add(c);
    return this;
  }

  public Biscuit addAuthorityCheck(String s) throws Error.Parser {
    Either<
            org.biscuitsec.biscuit.token.builder.parser.Error,
            Tuple2<String, org.biscuitsec.biscuit.token.builder.Check>>
        res = Parser.check(s);

    if (res.isLeft()) {
      throw new Error.Parser(res.getLeft());
    }

    Tuple2<String, org.biscuitsec.biscuit.token.builder.Check> t = res.get();

    return addAuthorityCheck(t._2);
  }

  public Biscuit setContext(String context) {
    this.context = context;
    return this;
  }

  public Biscuit addScope(org.biscuitsec.biscuit.token.builder.Scope scope) {
    this.scopes.add(scope);
    return this;
  }

  public void setRootKeyId(Integer i) {
    this.rootKeyId = Option.some(i);
  }

  public org.biscuitsec.biscuit.token.Biscuit build() throws Error {
    return build(defaultSymbolTable());
  }

  private org.biscuitsec.biscuit.token.Biscuit build(SymbolTable symbolTable) throws Error {
    final int symbolStart = symbolTable.currentOffset();
    final int publicKeyStart = symbolTable.currentPublicKeyOffset();

    List<org.biscuitsec.biscuit.datalog.Fact> facts = new ArrayList<>();
    for (Fact f : this.facts) {
      facts.add(f.convert(symbolTable));
    }
    List<org.biscuitsec.biscuit.datalog.Rule> rules = new ArrayList<>();
    for (Rule r : this.rules) {
      rules.add(r.convert(symbolTable));
    }
    List<org.biscuitsec.biscuit.datalog.Check> checks = new ArrayList<>();
    for (Check c : this.checks) {
      checks.add(c.convert(symbolTable));
    }
    List<org.biscuitsec.biscuit.datalog.Scope> scopes = new ArrayList<>();
    for (Scope s : this.scopes) {
      scopes.add(s.convert(symbolTable));
    }
    SchemaVersion schemaVersion = new SchemaVersion(facts, rules, checks, scopes);

    SymbolTable blockSymbols = new SymbolTable();

    for (int i = symbolStart; i < symbolTable.symbols().size(); i++) {
      blockSymbols.add(symbolTable.symbols().get(i));
    }

    List<PublicKey> publicKeys = new ArrayList<>();
    for (int i = publicKeyStart; i < symbolTable.currentPublicKeyOffset(); i++) {
      publicKeys.add(symbolTable.getPublicKeys().get(i));
    }

    Block authorityBlock =
        new Block(
            blockSymbols,
            context,
            facts,
            rules,
            checks,
            scopes,
            publicKeys,
            Option.none(),
            schemaVersion.version());

    if (this.rootKeyId.isDefined()) {
      return org.biscuitsec.biscuit.token.Biscuit.make(
          this.rng, this.root, this.rootKeyId.get(), authorityBlock);
    } else {
      return org.biscuitsec.biscuit.token.Biscuit.make(this.rng, this.root, authorityBlock);
    }
  }

  public Biscuit addRight(String resource, String right) throws Error.Language {
    return this.addAuthorityFact(
        Utils.fact("right", Arrays.asList(Utils.string(resource), Utils.str(right))));
  }
}
