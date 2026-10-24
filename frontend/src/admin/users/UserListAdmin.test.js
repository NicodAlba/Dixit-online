import { render, screen, waitFor } from "../../test-utils";
import userEvent from "@testing-library/user-event";
import UserListAdmin from "./UserListAdmin";

describe('UserListAdmin', () => {
    test('renders correctly', async () => {
        render(<UserListAdmin />);
        expect(screen.getByRole('heading', { name: 'Users' })).toBeInTheDocument();
        expect(screen.getByRole('table', { name: 'users' })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'add-user' })).toHaveAttribute('href', '/users/new');
        expect(screen.getAllByRole('row')).toHaveLength(1);
    });

    test('renders users correctly', async () => {
        render(<UserListAdmin />);
        const owner1 = await screen.findByRole('cell', { 'name': 'owner1' });
        expect(owner1).toBeInTheDocument();

        const editButtons = await screen.findAllByRole('link', { 'name': /edit/ });
        expect(editButtons).toHaveLength(2);

        const deleteButtons = await screen.findAllByRole('button', { 'name': /delete/ });
        expect(deleteButtons).toHaveLength(2);

        const admin1 = await screen.findByRole('cell', { 'name': 'admin1' });
        expect(admin1).toBeInTheDocument();

        const owners = await screen.findAllByRole('row', {},);
        expect(owners).toHaveLength(3);
    });

    test('delete user correct', async () => {
        const user = userEvent.setup();
        const confirm = jest.spyOn(window, 'confirm').mockReturnValue(true);
        try {
            render(<UserListAdmin />);
            const user1Delete = await screen.findByRole('button', { 'name': 'delete-1' });
            await user.click(user1Delete);
            expect(confirm).toHaveBeenCalledTimes(1);
            expect(await screen.findByRole('alert')).toBeInTheDocument();
            await waitFor(() => expect(screen.queryByRole('cell', { name: 'admin1' })).not.toBeInTheDocument());
            expect(screen.getByRole('cell', { name: 'owner1' })).toBeInTheDocument();
        } finally {
            confirm.mockRestore();
        }
    });
});
