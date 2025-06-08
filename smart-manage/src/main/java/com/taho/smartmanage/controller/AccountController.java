package com.taho.smartmanage.controller;


import java.net.Authenticator;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.taho.entity.Account;
import com.example.taho.entity.UserProfile;
import com.example.taho.service.AccountService;
import com.example.taho.service.ProfileService;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 * コントローラークラス
 */

@Controller
public class AccountController {

	@Autowired
	private ProfileService profileService;


	private final AccountService service;


	public AccountController(AccountService service) {
		this.service = service;
	}

	// 全件表示を行う
	@GetMapping("/account")
	public String showHomePage(Model model, Principal principal) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName(); // ← ここで username を取得！

		UserProfile profile = profileService.getProfileByUsername(username); // ← これでOK！

		if (profile != null) {
			model.addAttribute("targetSaving", profile.getTargetSaving());
			model.addAttribute("targetExpensing", profile.getTargetExpensing());
		}

		List<Account> list = service.findByUsername(username);
		int totalIncome = list.stream().filter(account -> account.getType() >= 10).mapToInt(Account::getPrice).sum();
		int totalPrice = list.stream().filter(account -> account.getType() < 10).mapToInt(Account::getPrice).sum();
		int balance = totalIncome - totalPrice;
		int thisMonthExpense = service.getCurrentMonthExpense(username);

		Map<String, Integer> expensesByCategory = service.getExpenseByCategory(username);

		model.addAttribute("thisMonthExpense", thisMonthExpense);
		model.addAttribute("totalIncome", totalIncome);
		model.addAttribute("totalPrice", totalPrice);
		model.addAttribute("balance", balance);
		model.addAttribute("expenseData", expensesByCategory);

		return "account/index"; 
	}

	

	@GetMapping("/list")
	public String showList(Model model) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();

		List<Account> list = service.findByUsername(username);

		int totalIncome = list.stream()
			.filter(account -> account.getType() >= 10)
			.mapToInt(Account::getPrice)
			.sum();

		int totalPrice = list.stream()
			.filter(account -> account.getType() < 10)
			.mapToInt(Account::getPrice)
			.sum();

		int balance = totalIncome - totalPrice;

		Map<String, Integer> expensesByCategory = service.getExpenseByCategory(username);

		model.addAttribute("list", list);
		model.addAttribute("totalPrice", totalPrice);
		model.addAttribute("totalIncome", totalIncome);
		model.addAttribute("expenseData", expensesByCategory);
		model.addAttribute("balance", balance);

		return "account/list";  // 🔸 list.html に飛ばす
	}

	@Autowired
    private AccountService accountService;
    
	@PostMapping("/add")
		public String addAccount(Account account) {
    	accountService.addAccount(account); // 新しいデータを追加
    	return "redirect:/account/"; // 一覧ページをリロードして新しいデータを表示
	}


	// 新規登録画面へ遷移
	@GetMapping("/account/insert")
	public String goInsert() {
		return "account/insert";
	}

	// 新規登録画面へ遷移
	@PostMapping("/account/insert")
	public String insert(
		Model model,
		@RequestParam("type") int recordType,      // 0: 支出, 10: 収入
		@RequestParam("category") int categoryType, // 選択されたカテゴリー（1〜9など）
		Account account
	) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();
		account.setUsername(username);
	
		// 🔁 type を決定
		int finalType = recordType == 0 ? categoryType : categoryType + 10;
		account.setType(finalType);
	
		service.insertAccount(account);  // insertAccount に保存を任せる
		model.addAttribute("account", account);
		return "account/insertComplete";
	}
	

	@GetMapping("/account/insertIncome")
	public String goInsertIncome() {
		return "account/insertIncome";
	}

	@PostMapping("/account/insertIncome")
	public String insertIncome(Model model, Account account){
	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

	account.setUsername(username);
    service.insertIncomeAccount(account);

	model.addAttribute("account", account);
    return "account/insertIncomeComplete";
	}

	// 収入の削除処理
	@PostMapping("/account/deleteIncome")
	public String deleteIncome(Model model, @RequestParam int id) {
    	// 削除前に収入データを取得
    	Account account = service.getAccountById(id);
    	if (account != null && account.getType() >= 10) { // 🔹 typeが収入(10以上)の場合のみ削除
        	model.addAttribute("account", account);
        	service.deleteAccountById(id);
    	}
    	return "redirect:/list"; 
	}



	// 収入更新画面へ遷移
	@GetMapping("/account/updateIncomeInput")
	public String updateIncomeInput(Model model, @RequestParam int id) {
		Account account = service.findAccountById(id);

		// "yyyy/MM/dd" → "yyyy-MM-dd" に変換して渡す
		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		String formattedDate = "";
		try {
			LocalDate parsedDate = LocalDate.parse(account.getDate(), inputFormatter);
			formattedDate = parsedDate.format(outputFormatter);
		} catch (Exception e) {
			formattedDate = LocalDate.now().format(outputFormatter); // fallback
		}

		model.addAttribute("account", account);
		model.addAttribute("formattedDate", formattedDate); // ← これを HTML 側で使う
		return "account/updateIncomeInput"; 
	}
	

	// 収入の更新処理
	@PostMapping("/account/updateIncome")
	public String updateIncome(Model model, Account account) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    	String username = authentication.getName();
    	account.setUsername(username);

    	if (account.getType() >= 10) { 
        	service.updateAccount(account);
        	model.addAttribute("account", account);
    	}
    	return "account/updateIncomeComplete";

	}
	
	// 削除処理を行う
	@PostMapping("/account/delete")
		public String delete(Model model, @RequestParam int id) {
    	// 削除前にアカウント情報を取得する
    	Account account = service.getAccountById(id);
    	if (account != null) {
        	model.addAttribute("account", account); // 削除したアカウント情報をModelに追加
    	}
    	service.deleteAccountById(id);
    	return "redirect:/list/"; 
	}

	@GetMapping("/account/updateInput")
	public String updateInput(Model model, @RequestParam int id) {
		Account account = service.findAccountById(id);

		// 日付を yyyy/MM/dd → yyyy-MM-dd に変換
		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		String formattedDate;
		try {
			LocalDate parsedDate = LocalDate.parse(account.getDate(), inputFormatter);
			formattedDate = parsedDate.format(outputFormatter);
		} catch (Exception e) {
			formattedDate = LocalDate.now().format(outputFormatter);
		}

		model.addAttribute("account", account);
		model.addAttribute("formattedDate", formattedDate); // ← これをHTMLで使う！

		return "account/updateInput";
	}

	
	// 更新処理を行う
	@PostMapping("/account/update")
	public String update(Model model, Account account) {
		// 🔹 認証ユーザーの username をセット！
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();
		account.setUsername(username);
	
		service.updateAccount(account); 
		model.addAttribute("account", account); 
		return "account/updateComplete"; 
	}
	

	@GetMapping("/account/search")
	public String search(@RequestParam(required = false) Integer year,
						 @RequestParam(required = false) Integer month,
						 @RequestParam(required = false) Integer type,
						 Model model) {
	
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();
	
		// 🔹 `type == null ? 9 : type` を削除（検索結果がおかしくなる原因）
		List<Account> list = service.searchAccounts(year, month, type, username);
	
		// 🔹 支出の合計を計算
		int totalPrice = list.stream()
							.filter(account -> account.getType() < 10) // 🔹 支出だけを計算
							.mapToInt(Account::getPrice)
							.sum();
	
		// 🔹 デバッグ用に検索条件と結果件数を表示
		System.out.println("検索条件: 年=" + year + ", 月=" + month + ", カテゴリー=" + type + ", ユーザー=" + username);
		System.out.println("検索結果件数: " + list.size());
	
		model.addAttribute("list", list);
		model.addAttribute("totalPrice", totalPrice);
	
		return "account/search";
	}

}