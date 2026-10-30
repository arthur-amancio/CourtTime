package br.com.courttime.controller;

import br.com.courttime.controller.form.CadastroUsuarioForm;
import br.com.courttime.controller.form.EdicaoUsuarioForm;
import br.com.courttime.controller.form.RedefinicaoSenhaForm;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Usuario;
import br.com.courttime.security.UsuarioAtual;
import br.com.courttime.service.AdministracaoUsuarioService;
import br.com.courttime.service.UsuarioResumo;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private final AdministracaoUsuarioService administracaoUsuarioService;

    public AdminUsuarioController(
            AdministracaoUsuarioService administracaoUsuarioService) {
        this.administracaoUsuarioService = administracaoUsuarioService;
    }

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "") String busca,
            @RequestParam(required = false) Perfil perfil,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "0") int pagina,
            @UsuarioAtual Usuario administrador,
            Model model) {
        Page<?> usuarios = administracaoUsuarioService.listar(
                administrador, busca, perfil, ativo, pagina);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("busca", busca);
        model.addAttribute("perfilSelecionado", perfil);
        model.addAttribute("ativoSelecionado", ativo);
        model.addAttribute("perfis", Perfil.values());
        model.addAttribute("operadorId", administrador.getId());
        return "admin/usuarios/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new CadastroUsuarioForm());
        model.addAttribute("perfis", administracaoUsuarioService.perfisCadastraveis());
        return "admin/usuarios/cadastro";
    }

    @PostMapping
    public String cadastrar(
            @Valid @ModelAttribute("form") CadastroUsuarioForm form,
            BindingResult bindingResult,
            @UsuarioAtual Usuario administrador,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepararCadastro(model);
            return "admin/usuarios/cadastro";
        }
        try {
            administracaoUsuarioService.cadastrar(
                    administrador,
                    form.getNome(),
                    form.getEmail(),
                    form.getPerfil(),
                    form.getSenha());
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Usuário cadastrado com sucesso.");
            return "redirect:/admin/usuarios";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("usuario.invalido", exception.getMessage());
            prepararCadastro(model);
            return "admin/usuarios/cadastro";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id, @UsuarioAtual Usuario administrador, Model model) {
        UsuarioResumo usuario = administracaoUsuarioService.obterParaEdicao(
                id, administrador);
        EdicaoUsuarioForm form = new EdicaoUsuarioForm();
        form.setNome(usuario.nome());
        form.setEmail(usuario.email());
        form.setPerfil(usuario.perfil());
        model.addAttribute("form", form);
        prepararEdicao(model, usuario);
        return "admin/usuarios/edicao";
    }

    @PostMapping("/{id}")
    public String salvarEdicao(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") EdicaoUsuarioForm form,
            BindingResult bindingResult,
            @UsuarioAtual Usuario administrador,
            Model model,
            RedirectAttributes redirectAttributes) {
        UsuarioResumo usuario = administracaoUsuarioService.obterParaEdicao(id, administrador);
        if (bindingResult.hasErrors()) {
            prepararEdicao(model, usuario);
            return "admin/usuarios/edicao";
        }
        try {
            administracaoUsuarioService.editar(
                    id,
                    administrador,
                    form.getNome(),
                    form.getEmail(),
                    form.getPerfil());
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Usuário atualizado com sucesso.");
            return "redirect:/admin/usuarios";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("usuario.invalido", exception.getMessage());
            prepararEdicao(model, usuario);
            return "admin/usuarios/edicao";
        }
    }

    @PostMapping("/{id}/alternar")
    public String alternar(
            @PathVariable Long id,
            @UsuarioAtual Usuario administrador,
            RedirectAttributes redirectAttributes) {
        try {
            var usuario = administracaoUsuarioService.alternarEstado(
                    id, administrador);
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    usuario.ativo() ? "Conta ativada com sucesso." : "Conta desativada.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/{id}/senha")
    public String senha(
            @PathVariable Long id, @UsuarioAtual Usuario administrador, Model model) {
        UsuarioResumo usuario = administracaoUsuarioService.obterParaEdicao(
                id, administrador);
        model.addAttribute("usuario", usuario);
        model.addAttribute("form", new RedefinicaoSenhaForm());
        return "admin/usuarios/senha";
    }

    @PostMapping("/{id}/senha")
    public String redefinirSenha(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") RedefinicaoSenhaForm form,
            BindingResult bindingResult,
            @UsuarioAtual Usuario administrador,
            Model model,
            RedirectAttributes redirectAttributes) {
        UsuarioResumo usuario = administracaoUsuarioService.obterParaEdicao(id, administrador);
        if (bindingResult.hasErrors()) {
            model.addAttribute("usuario", usuario);
            return "admin/usuarios/senha";
        }
        try {
            administracaoUsuarioService.redefinirSenha(
                    id, administrador, form.getNovaSenha());
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Senha redefinida. As sessões anteriores foram invalidadas.");
            return "redirect:/admin/usuarios";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("senha.invalida", exception.getMessage());
            model.addAttribute("usuario", usuario);
            return "admin/usuarios/senha";
        }
    }


    private void prepararCadastro(Model model) {
        model.addAttribute("perfis", administracaoUsuarioService.perfisCadastraveis());
    }

    private void prepararEdicao(Model model, UsuarioResumo usuario) {
        List<Perfil> perfis = usuario.perfil() == Perfil.ADMINISTRADOR
                ? List.of(Perfil.ADMINISTRADOR)
                : administracaoUsuarioService.perfisCadastraveis();
        model.addAttribute("usuario", usuario);
        model.addAttribute("perfis", perfis);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String tratarUsuarioNaoEncontrado(
            IllegalArgumentException exception,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
        return "redirect:/admin/usuarios";
    }
}
