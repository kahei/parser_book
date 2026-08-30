package parser.slr1;

import java.util.*;
import java.util.stream.Collectors;

public class SLR1Parser {
    private final Grammar grammar;
    private final Rule augmentedStartRule; // 拡張開始規則を保持
    private final List<LR0ItemSet> states;
    private final Map<LR0ItemSet, Map<Expression, LR0ItemSet>> transitions;
    private final Map<String, Set<Expression>> followSets;
    private final Map<String, Set<Expression>> firstSets = new HashMap<>();
    // EPSILONは内部計算用の空生成記号として扱います
    private static final Expression EPSILON = new Expression.Terminal("ε");

    public SLR1Parser(Grammar grammar) {
        // 拡張文法の作成（新たな開始記号を追加）
        List<Rule> extendedRules = new ArrayList<>(grammar.rules());
        String newStart = grammar.start() + "'";
        Rule augmented = new Rule(newStart, List.of(new Expression.NonTerminal(grammar.start())));
        extendedRules.add(0, augmented);
        this.augmentedStartRule = augmented;
        this.grammar = new Grammar(newStart, extendedRules);
        this.states = new ArrayList<>();
        this.transitions = new HashMap<>();
        buildStates();
        // Follow集合を計算
        this.followSets = computeFollowSets();
        // 構文解析表の競合をチェック
        checkConflicts();
    }

    private void buildStates() {
        Rule startRule = grammar.rules().get(0);
        System.out.println("startRule: " + startRule);
        LR0Item startItem = new LR0Item(startRule, 0);
        LR0ItemSet startState = new LR0ItemSet(Set.of(startItem)).closure(grammar);
        states.add(startState);

        Queue<LR0ItemSet> worklist = new ArrayDeque<>();
        worklist.add(startState);

        while (!worklist.isEmpty()) {
            LR0ItemSet state = worklist.poll();
            Map<Expression, Set<LR0Item>> itemsBySymbol = new HashMap<>();
            
            // 同じ記号で遷移するアイテムをグループ化
            for (LR0Item item : state.items()) {
                Expression next = item.nextSymbol();
                if (next != null) {
                    itemsBySymbol.computeIfAbsent(next, k -> new HashSet<>()).add(item);
                }
            }
            
            // 各記号に対して遷移先の状態を計算
            Map<Expression, LR0ItemSet> stateTransitions = new HashMap<>();
            for (Map.Entry<Expression, Set<LR0Item>> entry : itemsBySymbol.entrySet()) {
                Expression symbol = entry.getKey();
                Set<LR0Item> items = entry.getValue();
                
                // 同じ記号で遷移するすべてのアイテムを進める
                Set<LR0Item> nextItems = new HashSet<>();
                for (LR0Item item : items) {
                    nextItems.add(item.advance());
                }
                
                // クロージャを計算
                LR0ItemSet nextState = new LR0ItemSet(nextItems).closure(grammar);
                
                // 新しい状態なら追加
                int existingIndex = states.indexOf(nextState);
                if (existingIndex == -1) {
                    states.add(nextState);
                    worklist.add(nextState);
                    stateTransitions.put(symbol, nextState);
                } else {
                    stateTransitions.put(symbol, states.get(existingIndex));
                }
            }
            
            transitions.put(state, stateTransitions);
        }
    }

    // Follow集合の計算（標準的なアルゴリズム）
    private Map<String, Set<Expression>> computeFollowSets() {
        Map<String, Set<Expression>> follow = new HashMap<>();
        // 文法中のすべての非終端記号について初期化
        for (Rule rule : grammar.rules()) {
            follow.putIfAbsent(rule.name(), new HashSet<>());
            for (Expression sym : rule.body()) {
                if (sym instanceof Expression.NonTerminal nt) {
                    follow.putIfAbsent(nt.name(), new HashSet<>());
                }
            }
        }
        // 拡張開始記号には入力終了記号（null）を追加
        follow.get(grammar.start()).add(null);

        boolean changed;
        do {
            changed = false;
            for (Rule rule : grammar.rules()) {
                List<Expression> body = rule.body();
                for (int i = 0; i < body.size(); i++) {
                    Expression symbol = body.get(i);
                    if (symbol instanceof Expression.NonTerminal nt) {
                        String ntName = nt.name();
                        Set<Expression> followSet = follow.get(ntName);
                        int oldSize = followSet.size();

                        List<Expression> beta = body.subList(i + 1, body.size());
                        Set<Expression> firstBeta = firstOfSequence(beta);
                        // FIRST(β)からEPSILONを除いたものを追加
                        followSet.addAll(firstBeta.stream()
                                .filter(s -> !s.equals(EPSILON))
                                .collect(Collectors.toSet()));
                        // βがEPSILONを生成する（または空列）の場合は、左辺のFollow集合も追加
                        if (beta.isEmpty() || firstBeta.contains(EPSILON)) {
                            followSet.addAll(follow.get(rule.name()));
                        }

                        if (followSet.size() > oldSize) {
                            changed = true;
                        }
                    }
                }
            }
        } while (changed);
        return follow;
    }

    // シーケンスのFIRST集合を計算
    private Set<Expression> firstOfSequence(List<Expression> symbols) {
        Set<Expression> result = new HashSet<>();
        if (symbols.isEmpty()) {
            result.add(EPSILON);
            return result;
        }
        for (Expression sym : symbols) {
            Set<Expression> firstSet = firstOf(sym);
            result.addAll(firstSet.stream()
                    .filter(s -> !s.equals(EPSILON))
                    .collect(Collectors.toSet()));
            if (!firstSet.contains(EPSILON)) {
                return result;
            }
        }
        result.add(EPSILON);
        return result;
    }

    // 単一記号のFIRST集合を計算（メモ化付き）
    private Set<Expression> firstOf(Expression symbol) {
        if (symbol instanceof Expression.Terminal) {
            return Set.of(symbol);
        } else if (symbol instanceof Expression.NonTerminal nt) {
            // キャッシュをチェック
            if (firstSets.containsKey(nt.name())) {
                return firstSets.get(nt.name());
            }
            
            // 計算中のマーカーとして空集合を設定（無限再帰防止）
            firstSets.put(nt.name(), new HashSet<>());
            
            Set<Expression> result = new HashSet<>();
            for (Rule rule : grammar.rules()) {
                if (rule.name().equals(nt.name())) {
                    if (rule.body().isEmpty()) {
                        result.add(EPSILON);
                    } else {
                        result.addAll(firstOfSequence(rule.body()));
                    }
                }
            }
            
            // 結果をキャッシュに保存
            firstSets.put(nt.name(), result);
            return result;
        }
        return Set.of();
    }

    // デバッグ用：FOLLOW集合を取得
    public Map<String, Set<Expression>> getFollowSets() {
        return followSets;
    }
    
    // 構文解析表の競合をチェック
    private void checkConflicts() {
        boolean hasConflict = false;
        StringBuilder conflictDetails = new StringBuilder();
        
        for (int i = 0; i < states.size(); i++) {
            LR0ItemSet state = states.get(i);
            Map<Expression, Set<String>> actions = new HashMap<>();
            
            // シフトアクションを収集
            Map<Expression, LR0ItemSet> stateTransitions = transitions.get(state);
            if (stateTransitions != null) {
                for (Map.Entry<Expression, LR0ItemSet> entry : stateTransitions.entrySet()) {
                    if (entry.getKey() instanceof Expression.Terminal) {
                        actions.computeIfAbsent(entry.getKey(), k -> new HashSet<>())
                                .add("shift to state " + states.indexOf(entry.getValue()));
                    }
                }
            }
            
            // リデュースアクションを収集
            for (LR0Item item : state.items()) {
                if (item.nextSymbol() == null && !item.rule().equals(augmentedStartRule)) {
                    Set<Expression> follow = followSets.get(item.rule().name());
                    if (follow != null) {
                        for (Expression lookahead : follow) {
                            actions.computeIfAbsent(lookahead, k -> new HashSet<>())
                                    .add("reduce by " + item.rule());
                        }
                    }
                }
            }
            
            // 競合を検出
            for (Map.Entry<Expression, Set<String>> entry : actions.entrySet()) {
                if (entry.getValue().size() > 1) {
                    hasConflict = true;
                    conflictDetails.append("Conflict in state ").append(i)
                            .append(" on symbol ").append(entry.getKey()).append(":\n");
                    for (String action : entry.getValue()) {
                        conflictDetails.append("  - ").append(action).append("\n");
                    }
                }
            }
        }
        
        if (hasConflict) {
            throw new IllegalStateException("SLR(1) conflicts detected:\n" + conflictDetails);
        }
    }
    
    public boolean parse(List<Expression> input) {
        Stack<LR0ItemSet> stack = new Stack<>();
        stack.push(states.get(0));

        int index = 0;
        int reductionCount = 0;
        final int MAX_REDUCTIONS = 1000; // 無限ループ防止
        
        while (index <= input.size() && reductionCount < MAX_REDUCTIONS) {
            LR0ItemSet currentState = stack.peek();
            // 入力が尽きた場合はnullを終端記号とみなす
            Expression currentSymbol = index < input.size() ? input.get(index) : null;

            System.out.println("Current State: " + currentState);
            System.out.println("Current Symbol: " + currentSymbol);

            Map<Expression, LR0ItemSet> stateTransitions = transitions.get(currentState);

            // シフト可能ならシフト
            if (currentSymbol != null && stateTransitions != null && stateTransitions.containsKey(currentSymbol)) {
                System.out.println("Shifting: " + currentSymbol);
                stack.push(stateTransitions.get(currentSymbol));
                index++;
            } else {
                boolean reduced = false;
                for (LR0Item item : currentState.items()) {
                    if (item.nextSymbol() == null) {  // 完了したアイテム
                        // 拡張開始規則の場合は、入力が完全に消費されたときのみ受理
                        if (item.rule().equals(augmentedStartRule)) {
                            if (currentSymbol == null) {
                                System.out.println("Accepting state reached with full input consumption.");
                                return true;
                            }
                        } else {
                            // リデュースを行うため、lookaheadがその規則の左辺のFollow集合に含まれている必要がある
                            Set<Expression> follow = followSets.get(item.rule().name());
                            if (follow != null && follow.contains(currentSymbol)) {
                                System.out.println("Reducing by rule: " + item.rule());
                                // 右辺の記号の数だけスタックからポップ
                                for (int i = 0; i < item.rule().body().size(); i++) {
                                    stack.pop();
                                }
                                // Goto遷移を行う
                                Map<Expression, LR0ItemSet> peekTransitions = transitions.get(stack.peek());
                                if (peekTransitions != null) {
                                    LR0ItemSet gotoState = peekTransitions.get(new Expression.NonTerminal(item.rule().name()));
                                    if (gotoState != null) {
                                        stack.push(gotoState);
                                        System.out.println("Goto state: " + gotoState);
                                        reduced = true;
                                        reductionCount++;
                                        break;
                                    }
                                }
                                if (!reduced) {
                                    System.out.println("No Goto state found for " + item.rule().name() + " from state " + stack.peek());
                                    return false;
                                }
                            }
                        }
                    }
                }
                if (!reduced) {
                    System.out.println("No valid shift or reduce action found. Parsing failed.");
                    return false;
                }
            }
        }

        return false;
    }
}