package parser.ll1;

import java.util.*;

public class LL1Recognizer {
    private final Grammar grammar;
    private final Map<String, Set<Expression.Terminal>> firstSets;
    private final Map<String, Set<Expression.Terminal>> followSets;
    private final Map<String, Map<Expression.Terminal, Rule>> parseTable;
    private static final Expression.Terminal EPSILON = new Expression.Terminal("ε");
    private static final Expression.Terminal EOF = new Expression.Terminal("$");
    
    public LL1Recognizer(Grammar grammar) {
        this.grammar = grammar;
        this.firstSets = new HashMap<>();
        this.followSets = new HashMap<>();
        this.parseTable = new HashMap<>();
        
        computeFirstSets();
        computeFollowSets();
        constructParseTable();
    }
    
    // FIRST集合の計算
    private void computeFirstSets() {
        initializeFirstSets();
        
        // 固定点に達するまで繰り返す
        boolean changed;
        do {
            changed = false;
            for (var rule : grammar.rules()) {
                if (updateFirstSet(rule)) {
                    changed = true;
                }
            }
        } while (changed);
    }
    
    // FIRST集合の初期化
    private void initializeFirstSets() {
        // 終端記号のFIRST集合は自分自身
        for (var rule : grammar.rules()) {
            for (var expr : rule.body()) {
                if (expr instanceof Expression.Terminal t) {
                    firstSets.computeIfAbsent(t.value(), k -> new HashSet<>()).add(t);
                }
            }
        }
        
        // 非終端記号のFIRST集合を初期化
        for (var rule : grammar.rules()) {
            firstSets.computeIfAbsent(rule.name(), k -> new HashSet<>());
        }
    }
    
    // 一つのルールのFIRST集合を更新
    private boolean updateFirstSet(Rule rule) {
        var first = firstSets.get(rule.name());
        int originalSize = first.size();
        
        if (rule.body().isEmpty()) {
            // 空規則の場合
            first.add(EPSILON);
        } else {
            // ルール本体のFIRST集合を計算
            var bodyFirst = computeFirstOfSequence(rule.body());
            first.addAll(bodyFirst);
        }
        
        return first.size() > originalSize;
    }
    
    // FOLLOW集合の計算
    private void computeFollowSets() {
        initializeFollowSets();
        
        // 固定点に達するまで繰り返す
        boolean changed;
        do {
            changed = false;
            for (var rule : grammar.rules()) {
                if (updateFollowSets(rule)) {
                    changed = true;
                }
            }
        } while (changed);
    }
    
    // FOLLOW集合の初期化
    private void initializeFollowSets() {
        // すべての非終端記号のFOLLOW集合を初期化
        for (var rule : grammar.rules()) {
            followSets.computeIfAbsent(rule.name(), k -> new HashSet<>());
        }
        
        // 開始記号のFOLLOW集合に$を追加
        followSets.get(grammar.start()).add(EOF);
    }
    
    // 一つのルールに対してFOLLOW集合を更新
    private boolean updateFollowSets(Rule rule) {
        boolean changed = false;
        
        for (int i = 0; i < rule.body().size(); i++) {
            var expr = rule.body().get(i);
            if (expr instanceof Expression.NonTerminal nt) {
                // A -> αBβ の形で、Bに対する処理
                var beta = rule.body().subList(i + 1, rule.body().size());
                if (updateFollowSetForNonTerminal(nt.name(), beta, rule.name())) {
                    changed = true;
                }
            }
        }
        
        return changed;
    }
    
    // 非終端記号のFOLLOW集合を更新
    private boolean updateFollowSetForNonTerminal(String nonTerminal, List<Expression> beta, String ruleName) {
        var follow = followSets.get(nonTerminal);
        int originalSize = follow.size();
        
        // βのFIRST集合を計算
        var betaFirst = computeFirstOfSequence(beta);
        
        // FIRST(β) - {ε} をFOLLOW(B)に追加
        for (var terminal : betaFirst) {
            if (!terminal.equals(EPSILON)) {
                follow.add(terminal);
            }
        }
        
        // βがεを導出する場合、FOLLOW(A)をFOLLOW(B)に追加
        if (betaFirst.contains(EPSILON)) {
            var lhsFollow = followSets.get(ruleName);
            follow.addAll(lhsFollow);
        }
        
        return follow.size() > originalSize;
    }
    
    // パーステーブルの構築
    private void constructParseTable() {
        for (var rule : grammar.rules()) {
            parseTable.computeIfAbsent(rule.name(), k -> new HashMap<>());
            addRuleToParseTable(rule);
        }
    }
    
    // ルールをパーステーブルに追加
    private void addRuleToParseTable(Rule rule) {
        var ruleFirst = computeFirstOfSequence(rule.body());
        
        // FIRST集合の各終端記号に対してエントリを追加
        for (var terminal : ruleFirst) {
            if (!terminal.equals(EPSILON)) {
                addParseTableEntry(rule.name(), terminal, rule);
            }
        }
        
        // εがFIRST集合に含まれる場合、FOLLOW集合の各終端記号に対してエントリを追加
        if (ruleFirst.contains(EPSILON)) {
            var follow = followSets.get(rule.name());
            for (var terminal : follow) {
                addParseTableEntry(rule.name(), terminal, rule);
            }
        }
    }
    
    // パーステーブルにエントリを追加（競合チェック付き）
    private void addParseTableEntry(String nonTerminal, Expression.Terminal terminal, Rule rule) {
        var existing = parseTable.get(nonTerminal).get(terminal);
        if (existing != null && !existing.equals(rule)) {
            System.err.println("LL(1) conflict at [" + nonTerminal + ", " + terminal + "]");
        }
        parseTable.get(nonTerminal).put(terminal, rule);
    }
    
    // 記号列のFIRST集合を計算
    private Set<Expression.Terminal> computeFirstOfSequence(List<Expression> sequence) {
        Set<Expression.Terminal> result = new HashSet<>();
        
        if (sequence.isEmpty()) {
            result.add(EPSILON);
            return result;
        }
        
        for (var expr : sequence) {
            var symbolFirst = getFirstOfSymbol(expr);
            
            // εを除いて追加
            result.addAll(symbolFirst.stream()
                .filter(t -> !t.equals(EPSILON))
                .toList());
            
            // この記号がεを導出しない場合は終了
            if (!symbolFirst.contains(EPSILON)) {
                return result;
            }
        }
        
        // すべての記号がεを導出する場合
        result.add(EPSILON);
        return result;
    }
    
    // 単一の記号のFIRST集合を取得
    private Set<Expression.Terminal> getFirstOfSymbol(Expression symbol) {
        if (symbol instanceof Expression.Terminal t) {
            return Set.of(t);
        } else if (symbol instanceof Expression.NonTerminal nt) {
            var ntFirst = firstSets.get(nt.name());
            return ntFirst != null ? new HashSet<>(ntFirst) : new HashSet<>();
        }
        return new HashSet<>();
    }
    
    // 入力を認識
    public boolean recognize(List<Expression.Terminal> input) {
        var inputQueue = new LinkedList<>(input);
        inputQueue.add(EOF);  // 入力の終わりを表す
        
        var stack = new LinkedList<Expression>();
        stack.push(EOF);  // スタックの底
        stack.push(new Expression.NonTerminal(grammar.start()));  // 開始記号
        
        System.out.println("=== LL(1) Recognition Process ===");
        
        while (!stack.peek().equals(EOF)) {
            var top = stack.peek();
            var currentInput = inputQueue.peek();
            
            System.out.printf("Stack top: %s, Input: %s%n", top, currentInput);
            
            if (top instanceof Expression.Terminal t) {
                if (t.equals(currentInput)) {
                    stack.pop();
                    inputQueue.poll();
                    System.out.println("Matched terminal: " + t);
                } else {
                    System.out.println("Mismatch - expected " + t + " but found " + currentInput);
                    return false;
                }
            } else if (top instanceof Expression.NonTerminal nt) {
                var rule = parseTable.get(nt.name()).get(currentInput);
                if (rule == null) {
                    System.out.println("No rule found for [" + nt.name() + ", " + currentInput + "]");
                    return false;
                }
                
                System.out.println("Applying rule: " + rule);
                stack.pop();
                
                // ルールの本体を逆順でスタックにプッシュ（空規則でない場合）
                if (!rule.body().isEmpty()) {
                    for (int i = rule.body().size() - 1; i >= 0; i--) {
                        stack.push(rule.body().get(i));
                    }
                }
            }
        }
        
        // スタックが空で、入力も終わりに達していれば受理
        boolean accepted = stack.peek().equals(EOF) && inputQueue.peek().equals(EOF);
        if (accepted) {
            System.out.println("Input accepted!");
        } else {
            System.out.println("Input rejected - remaining input: " + inputQueue);
        }
        
        return accepted;
    }
    
    // デバッグ用メソッド
    public void printFirstSets() {
        System.out.println("=== FIRST Sets ===");
        for (var entry : firstSets.entrySet()) {
            System.out.println("FIRST(" + entry.getKey() + ") = " + entry.getValue());
        }
    }
    
    public void printFollowSets() {
        System.out.println("\n=== FOLLOW Sets ===");
        for (var entry : followSets.entrySet()) {
            System.out.println("FOLLOW(" + entry.getKey() + ") = " + entry.getValue());
        }
    }
    
    public void printParseTable() {
        System.out.println("\n=== LL(1) Parse Table ===");
        for (var ntEntry : parseTable.entrySet()) {
            for (var tEntry : ntEntry.getValue().entrySet()) {
                System.out.println("[" + ntEntry.getKey() + ", " + tEntry.getKey() + "] = " + tEntry.getValue());
            }
        }
    }
}