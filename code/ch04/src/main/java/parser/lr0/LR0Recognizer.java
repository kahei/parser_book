package parser.lr0;

import java.util.*;

import static java.util.List.of;

public class LR0Recognizer {
    private final Grammar augmentedGrammar;
    private final List<LR0ItemSet> states;
    private final Map<Integer, Map<Expression, Action>> actionTable;
    private final Map<Integer, Map<String, Integer>> gotoTable;

    public LR0Recognizer(Grammar grammar) {
        // 拡大文法を作成
        this.augmentedGrammar = createAugmentedGrammar(grammar);
        
        // 状態を構築
        this.states = constructStates();
        
        // アクションテーブルとGOTOテーブルを構築
        this.actionTable = new HashMap<>();
        this.gotoTable = new HashMap<>();
        constructTables();
    }

    private Grammar createAugmentedGrammar(Grammar grammar) {
        // S' -> S のルールを追加
        var newStart = "S'";
        var newRule = new Rule(newStart, of(new Expression.NonTerminal(grammar.start())));
        var augmentedRules = new ArrayList<Rule>();
        augmentedRules.add(newRule);
        augmentedRules.addAll(grammar.rules());
        return new Grammar(newStart, augmentedRules);
    }

    private List<LR0ItemSet> constructStates() {
        var states = new ArrayList<LR0ItemSet>();
        var stateMap = new HashMap<LR0ItemSet, Integer>();
        
        // 初期状態を作成
        var initialItem = new LR0Item(augmentedGrammar.rules().get(0), 0);
        var initialState = new LR0ItemSet(Set.of(initialItem)).closure(augmentedGrammar);
        
        var worklist = new LinkedList<LR0ItemSet>();
        worklist.add(initialState);
        states.add(initialState);
        stateMap.put(initialState, 0);
        
        while (!worklist.isEmpty()) {
            var state = worklist.poll();
            var transitions = computeTransitions(state);
            
            for (var nextState : transitions.values()) {
                if (!stateMap.containsKey(nextState)) {
                    states.add(nextState);
                    stateMap.put(nextState, states.size() - 1);
                    worklist.add(nextState);
                }
            }
        }
        
        return states;
    }

    private Map<Expression, LR0ItemSet> computeTransitions(LR0ItemSet state) {
        var transitions = new HashMap<Expression, LR0ItemSet>();
        var itemsBySymbol = new HashMap<Expression, Set<LR0Item>>();
        
        // シンボルごとにアイテムをグループ化
        for (var item : state.items()) {
            var symbol = item.nextSymbol();
            if (symbol != null) {
                itemsBySymbol.computeIfAbsent(symbol, k -> new HashSet<>()).add(item);
            }
        }
        
        // 各シンボルに対して遷移先の状態を計算
        for (var entry : itemsBySymbol.entrySet()) {
            var symbol = entry.getKey();
            var items = entry.getValue();
            var nextItems = new HashSet<LR0Item>();
            
            for (var item : items) {
                nextItems.add(item.advance());
            }
            
            var nextState = new LR0ItemSet(nextItems).closure(augmentedGrammar);
            transitions.put(symbol, nextState);
        }
        
        return transitions;
    }

    private void constructTables() {
        for (int i = 0; i < states.size(); i++) {
            var state = states.get(i);
            actionTable.put(i, new HashMap<>());
            gotoTable.put(i, new HashMap<>());
            
            var transitions = computeTransitions(state);
            
            // シフトアクションとGOTOを設定
            for (var entry : transitions.entrySet()) {
                var symbol = entry.getKey();
                var nextState = entry.getValue();
                var nextStateIndex = states.indexOf(nextState);
                
                if (symbol instanceof Expression.Terminal) {
                    actionTable.get(i).put(symbol, new Action.Shift(nextStateIndex));
                } else if (symbol instanceof Expression.NonTerminal nt) {
                    gotoTable.get(i).put(nt.name(), nextStateIndex);
                }
            }
            
            // リデュースアクションと受理アクションを設定
            for (var item : state.items()) {
                if (item.nextSymbol() == null) {  // ドットが最後にある
                    var rule = item.rule();
                    if (rule.name().equals("S'")) {
                        // 受理アクション（終端記号$に対して）
                        actionTable.get(i).put(null, new Action.Accept());
                    } else {
                        // リデュースアクション
                        // LR(0)では、すべての入力記号に対してリデュースを設定
                        var ruleIndex = augmentedGrammar.rules().indexOf(rule);
                        var reduceAction = new Action.Reduce(ruleIndex, rule);
                        
                        // すべての終端記号を収集
                        var terminals = new HashSet<Expression.Terminal>();
                        for (var r : augmentedGrammar.rules()) {
                            for (var expr : r.body()) {
                                if (expr instanceof Expression.Terminal t) {
                                    terminals.add(t);
                                }
                            }
                        }
                        
                        // すべての終端記号に対してリデュースアクションを設定
                        for (var terminal : terminals) {
                            var existing = actionTable.get(i).get(terminal);
                            if (existing != null && !existing.equals(reduceAction)) {
                                // シフト/リデュース競合またはリデュース/リデュース競合
                                System.err.println("Conflict at state " + i + " for symbol " + terminal);
                            } else {
                                actionTable.get(i).put(terminal, reduceAction);
                            }
                        }
                        
                        // 入力の終わり（null）に対してもリデュースアクションを設定
                        var existingEof = actionTable.get(i).get(null);
                        if (existingEof != null && !existingEof.equals(reduceAction)) {
                            System.err.println("Conflict at state " + i + " for EOF");
                        } else {
                            actionTable.get(i).put(null, reduceAction);
                        }
                    }
                }
            }
        }
    }

    public boolean recognize(List<Expression.Terminal> input) {
        var inputQueue = new LinkedList<>(input);
        inputQueue.add(null);  // 入力の終わりを表す
        
        var stack = new LinkedList<Integer>();
        stack.push(0);  // 初期状態
        
        System.out.println("=== LR(0) Recognition Process ===");
        
        while (!inputQueue.isEmpty()) {
            var currentState = stack.peek();
            var currentSymbol = inputQueue.peek();
            var action = actionTable.get(currentState).get(currentSymbol);
            
            System.out.printf("State: %d, Symbol: %s, Action: %s%n", 
                currentState, currentSymbol, action);
            
            if (action == null) {
                System.out.println("No action found - recognition failed");
                return false;
            }
            
            switch (action) {
                case Action.Shift shift -> {
                    inputQueue.poll();
                    stack.push(shift.state());
                    System.out.println("Shifted to state " + shift.state());
                }
                case Action.Reduce reduce -> {
                    var rule = reduce.rule();
                    // スタックからルールの本体の長さ分ポップ
                    for (int j = 0; j < rule.body().size(); j++) {
                        stack.pop();
                    }
                    
                    var gotoState = gotoTable.get(stack.peek()).get(rule.name());
                    if (gotoState == null) {
                        System.out.println("No goto found for " + rule.name() + " - recognition failed");
                        return false;
                    }
                    
                    stack.push(gotoState);
                    System.out.println("Reduced using rule: " + rule);
                }
                case Action.Accept accept -> {
                    System.out.println("Input accepted!");
                    return true;
                }
            }
        }
        
        return false;
    }

    // アクションの種類を表す型
    public sealed interface Action {
        record Shift(int state) implements Action {}
        record Reduce(int ruleIndex, Rule rule) implements Action {}
        record Accept() implements Action {}
    }

    // デバッグ用メソッド
    public void printStates() {
        System.out.println("=== LR(0) States ===");
        for (int i = 0; i < states.size(); i++) {
            System.out.println("State " + i + ":");
            System.out.println(states.get(i));
        }
    }

    public void printTables() {
        System.out.println("\n=== Action Table ===");
        for (int i = 0; i < states.size(); i++) {
            System.out.print("State " + i + ": ");
            var actions = actionTable.get(i);
            for (var entry : actions.entrySet()) {
                System.out.print(entry.getKey() + "=" + entry.getValue() + " ");
            }
            System.out.println();
        }
        
        System.out.println("\n=== Goto Table ===");
        for (int i = 0; i < states.size(); i++) {
            var gotos = gotoTable.get(i);
            if (!gotos.isEmpty()) {
                System.out.print("State " + i + ": ");
                for (var entry : gotos.entrySet()) {
                    System.out.print(entry.getKey() + "=" + entry.getValue() + " ");
                }
                System.out.println();
            }
        }
    }
}