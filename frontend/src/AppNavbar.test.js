import AppNavbar from "./AppNavbar";
import { fireEvent, render, screen } from "./test-utils";
import tokenService from "./services/token.service";

jest.mock("./services/token.service", () => ({
    getLocalAccessToken: jest.fn(),
}));

function signInAs(role) {
    const payload = btoa(JSON.stringify({ authorities: [role], sub: "dixit-player", id: 42 }));
    tokenService.getLocalAccessToken.mockReturnValue(`header.${payload}.signature`);
}

function expectLink(name, href) {
    expect(screen.getByRole("link", { name })).toHaveAttribute("href", href);
}

describe("AppNavbar", () => {
    beforeEach(() => tokenService.getLocalAccessToken.mockReturnValue(null));

    test("renders the public documentation and home links", () => {
        render(<AppNavbar />);
        expectLink("Docs", "/docs");
        expectLink("Dixit home", "/");
    });

    test("offers registration and login to visitors without private links", () => {
        render(<AppNavbar />);
        expectLink("Create account", "/register");
        expectLink("Log in", "/login");
        expect(screen.queryByRole("link", { name: "Your profile" })).not.toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Users" })).not.toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Logout" })).not.toBeInTheDocument();
    });

    test("the mobile menu exposes and toggles its expanded state", () => {
        render(<AppNavbar />);
        const toggle = screen.getByRole("button", {name: "Toggle navigation"});
        expect(toggle).toHaveAttribute("aria-controls", "main-navigation");
        expect(toggle).toHaveAttribute("aria-expanded", "false");
        fireEvent.click(toggle);
        expect(toggle).toHaveAttribute("aria-expanded", "true");
        fireEvent.click(toggle);
        expect(toggle).toHaveAttribute("aria-expanded", "false");
    });

    test("offers account and game links to a player", () => {
        signInAs("PLAYER");
        render(<AppNavbar />);
        expectLink("Docs", "/docs");
        expectLink("Your profile", "/users/42");
        expectLink("Achievements", "/users/42/achievements");
        expectLink("Statistics", "/stats");
        expectLink("Friends", "/friends");
        expectLink("Logout", "/logout");
        expect(screen.queryByRole("link", { name: "Users" })).not.toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "History" })).not.toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Log in" })).not.toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Create account" })).not.toBeInTheDocument();
    });

    test("offers administration links to an administrator", () => {
        signInAs("ADMIN");
        render(<AppNavbar />);
        expectLink("Users", "/users");
        expectLink("History", "/games/History");
        expectLink("Your profile", "/users/42");
        expectLink("Docs", "/docs");
        expectLink("Logout", "/logout");
        expect(screen.queryByRole("link", { name: "Friends" })).not.toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Achievements" })).not.toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Statistics" })).not.toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Create account" })).not.toBeInTheDocument();
    });
});

test("choosing a destination closes the mobile navigation, including the current route",()=>{
 tokenService.getLocalAccessToken.mockReturnValue(null);render(<AppNavbar/>);
 const toggle=screen.getByRole("button",{name:"Toggle navigation"});
 fireEvent.click(toggle);expect(toggle).toHaveAttribute("aria-expanded","true");
 fireEvent.click(screen.getByRole("link",{name:"Docs"}));expect(toggle).toHaveAttribute("aria-expanded","false");
 fireEvent.click(toggle);fireEvent.click(screen.getByRole("link",{name:"Docs"}));expect(toggle).toHaveAttribute("aria-expanded","false");
});
